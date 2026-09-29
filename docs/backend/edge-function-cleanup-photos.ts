// =============================================================================
// SUPABASE EDGE FUNCTION: clean-expired-photos
// =============================================================================
// Arquivo: docs/backend/edge-function-cleanup-photos.ts
// Localização de Deploy: supabase/functions/clean-expired-photos/index.ts
// Runtime: Deno / TypeScript (Supabase Edge Runtime)
// Descrição:
//   Executa a limpeza programada de fotos de etiquetas expiradas (photo_expires_at <= now())
//   de paradas vinculadas a rotas concluídas/canceladas (NUNCA em_andamento).
//   Remove os arquivos físicos do bucket 'master-route-photos' e limpa os campos no PostgreSQL.
// Referência: ADR-003 e Prompt 4 (Plano de Implementação Final)
// =============================================================================

import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";

const BUCKET_NAME = "master-route-photos";
const DEFAULT_BATCH_SIZE = 100;

interface RequestPayload {
    batch_size?: number;
    dry_run?: boolean;
}

interface ExpiredStopRecord {
    stop_id: string;
    photo_url: string;
    route_id: string;
    route_status: string;
    photo_expires_at: string;
}

serve(async (req: Request) => {
    // 1. Configuração de CORS
    if (req.method === "OPTIONS") {
        return new Response("ok", {
            headers: {
                "Access-Control-Allow-Origin": "*",
                "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
            },
        });
    }

    try {
        // 2. Validação das Chaves de Ambiente do Supabase
        const supabaseUrl = Deno.env.get("SUPABASE_URL");
        const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

        if (!supabaseUrl || !serviceRoleKey) {
            throw new Error("SUPABASE_URL ou SUPABASE_SERVICE_ROLE_KEY não configuradas no ambiente.");
        }

        // 3. Validação de Autorização (Proteção por Service Role ou Bearer Token)
        const authHeader = req.headers.get("Authorization");
        if (!authHeader || !authHeader.startsWith("Bearer ")) {
            return new Response(
                JSON.stringify({ error: "Autorização ausente. Necessário Bearer <SERVICE_ROLE_KEY>." }),
                { status: 401, headers: { "Content-Type": "application/json" } }
            );
        }

        const supabase = createClient(supabaseUrl, serviceRoleKey);

        // 4. Leitura dos Parâmetros da Requisição
        let batchSize = DEFAULT_BATCH_SIZE;
        let isDryRun = false;

        if (req.method === "POST") {
            try {
                const body: RequestPayload = await req.json();
                if (body.batch_size && body.batch_size > 0) {
                    batchSize = Math.min(body.batch_size, 500); // Limite de 500 por execução
                }
                if (body.dry_run !== undefined) {
                    isDryRun = Boolean(body.dry_run);
                }
            } catch {
                // Corpo JSON opcional
            }
        }

        console.log(`[clean-expired-photos] Iniciando rotina. BatchSize: ${batchSize}, DryRun: ${isDryRun}`);

        // 5. Consulta de Paradas Vencidas via RPC get_expired_photos_to_clean
        // A função SQL garante que rotas com status = 'em_andamento' NUNCA sejam selecionadas.
        const { data: expiredRecords, error: fetchError } = await supabase.rpc(
            "get_expired_photos_to_clean",
            { p_batch_size: batchSize }
        );

        if (fetchError) {
            console.error("[clean-expired-photos] Erro ao buscar fotos expiradas:", fetchError);
            throw new Error(`Falha no banco de dados ao buscar paradas: ${fetchError.message}`);
        }

        const records: ExpiredStopRecord[] = (expiredRecords as ExpiredStopRecord[]) || [];
        console.log(`[clean-expired-photos] Paradas elegíveis encontradas: ${records.length}`);

        if (records.length === 0) {
            return new Response(
                JSON.stringify({
                    success: true,
                    message: "Nenhuma foto expirada encontrada para limpeza.",
                    photos_found: 0,
                    photos_deleted: 0,
                    dry_run: isDryRun,
                }),
                { status: 200, headers: { "Content-Type": "application/json" } }
            );
        }

        // 6. Extração dos Caminhos de Arquivo no Supabase Storage
        const filePathsToDelete: string[] = [];
        const stopIdsToDelete: string[] = [];

        for (const item of records) {
            const rawUrl = item.photo_url;
            if (!rawUrl) continue;

            stopIdsToDelete.push(item.stop_id);

            // Extrai o caminho relativo dentro do bucket a partir da URL pública ou assinada
            // Exemplo 1: https://<id>.supabase.co/storage/v1/object/public/master-route-photos/routes/abc/stop1.jpg -> routes/abc/stop1.jpg
            // Exemplo 2: routes/abc/stop1.jpg
            let filePath = rawUrl;
            const bucketPrefix = `/storage/v1/object/public/${BUCKET_NAME}/`;
            const bucketSignPrefix = `/storage/v1/object/sign/${BUCKET_NAME}/`;

            if (filePath.includes(bucketPrefix)) {
                filePath = filePath.split(bucketPrefix)[1];
            } else if (filePath.includes(bucketSignPrefix)) {
                filePath = filePath.split(bucketSignPrefix)[1]?.split("?")[0] || filePath;
            } else if (filePath.startsWith(`${BUCKET_NAME}/`)) {
                filePath = filePath.substring(BUCKET_NAME.length + 1);
            }

            if (filePath) {
                filePathsToDelete.push(filePath);
            }
        }

        console.log(`[clean-expired-photos] Arquivos mapeados para exclusão no bucket '${BUCKET_NAME}':`, filePathsToDelete);

        if (isDryRun) {
            return new Response(
                JSON.stringify({
                    success: true,
                    mode: "dry_run",
                    message: `Modo Simulação: ${records.length} fotos seriam excluídas.`,
                    photos_found: records.length,
                    file_paths: filePathsToDelete,
                    stop_ids: stopIdsToDelete,
                }),
                { status: 200, headers: { "Content-Type": "application/json" } }
            );
        }

        // 7. Exclusão Física no Storage
        let deletedStorageCount = 0;
        if (filePathsToDelete.length > 0) {
            const { data: storageResult, error: storageError } = await supabase.storage
                .from(BUCKET_NAME)
                .remove(filePathsToDelete);

            if (storageError) {
                console.warn("[clean-expired-photos] Aviso/erro ao remover arquivos do Storage:", storageError);
            } else {
                deletedStorageCount = storageResult?.length || filePathsToDelete.length;
                console.log(`[clean-expired-photos] Arquivos físicos removidos do Storage: ${deletedStorageCount}`);
            }
        }

        // 8. Baixa no Banco de Dados via RPC mark_photos_as_cleaned
        const { data: updatedCount, error: updateError } = await supabase.rpc(
            "mark_photos_as_cleaned",
            {
                p_stop_ids: stopIdsToDelete,
                p_status: "sucesso",
                p_details: {
                    bucket: BUCKET_NAME,
                    deleted_storage_files: filePathsToDelete,
                },
            }
        );

        if (updateError) {
            console.error("[clean-expired-photos] Erro ao marcar fotos como limpas no banco:", updateError);
            throw new Error(`Fotos excluídas do Storage, mas erro ao atualizar banco: ${updateError.message}`);
        }

        console.log(`[clean-expired-photos] Sucesso! ${updatedCount} paradas atualizadas (photo_url = null).`);

        // 9. Retorno com Métricas e Auditoria
        return new Response(
            JSON.stringify({
                success: true,
                message: `Rotina executada com sucesso. ${updatedCount} fotos limpas.`,
                photos_found: records.length,
                photos_deleted: deletedStorageCount,
                stops_updated: updatedCount,
                cleaned_stop_ids: stopIdsToDelete,
                deleted_paths: filePathsToDelete,
                executed_at: new Date().toISOString(),
            }),
            { status: 200, headers: { "Content-Type": "application/json" } }
        );
    } catch (err: any) {
        console.error("[clean-expired-photos] Erro fatal durante a execução:", err);
        return new Response(
            JSON.stringify({
                success: false,
                error: err.message || "Erro desconhecido durante a limpeza de fotos.",
            }),
            { status: 500, headers: { "Content-Type": "application/json" } }
        );
    }
});
