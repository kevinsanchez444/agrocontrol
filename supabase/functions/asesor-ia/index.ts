// Edge Function: asesor-ia — proxy seguro hacia Gemini.
// La clave del proveedor vive como secreto del proyecto (GEMINI_API_KEY);
// nunca viaja en el APK.
const CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const PROMPT_SISTEMA = `Eres el asesor agronómico de AgroControl. Respondes SIEMPRE en español,
de forma práctica y breve (máximo 120 palabras), con viñetas, sobre café, plátano y aguacate:
plagas, riego, fertilización y manejo del lote. Cuando no estés seguro, dilo. Recuerda siempre
respetar la etiqueta de los productos y consultar a un ingeniero agrónomo antes de aplicar
insumos. No inventes precios ni datos regulatorios.`;

const MODELO = Deno.env.get("GEMINI_MODEL") ?? "gemini-3.5-flash-lite";

function json(obj: unknown, status = 200): Response {
  return new Response(JSON.stringify(obj), {
    status,
    headers: { ...CORS, "Content-Type": "application/json" },
  });
}

// Contexto (Fase 4): lotes del usuario; best-effort, nunca rompe la respuesta.
async function contextoLotes(usuarioId: string): Promise<string> {
  if (!usuarioId) return "";
  const base = Deno.env.get("SUPABASE_URL") ?? "";
  const clave = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
  if (!base || !clave) return "";
  try {
    const r = await fetch(
      `${base}/rest/v1/lotes?select=nombre,cultivo,hectareas&usuario_id=eq.${usuarioId}&limit=10`,
      { headers: { apikey: clave, Authorization: `Bearer ${clave}` } },
    );
    if (!r.ok) return "";
    const lotes = await r.json();
    if (!Array.isArray(lotes) || lotes.length === 0) return "";
    const lista = lotes
      .map((l: Record<string, unknown>) => `- ${l.nombre} (${l.cultivo}, ${l.hectareas} ha)`)
      .join("\n");
    return `\nLotes registrados por el usuario:\n${lista}\n`;
  } catch (_e) {
    return "";
  }
}

Deno.serve(async (req: Request): Promise<Response> => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: CORS });
  if (req.method !== "POST") return json({ error: "Método no permitido." }, 405);

  try {
    const cuerpo = await req.json().catch(() => null);
    const mensaje = cuerpo?.mensaje;
    const historial = Array.isArray(cuerpo?.historial) ? cuerpo.historial : [];
    const usuarioId = typeof cuerpo?.usuario_id === "string" ? cuerpo.usuario_id : "";

    if (typeof mensaje !== "string" || mensaje.trim().length === 0) {
      return json({ error: "Falta el mensaje." }, 400);
    }

    const claveProveedor = Deno.env.get("GEMINI_API_KEY");
    if (!claveProveedor) return json({ error: "Falta la clave del proveedor." }, 500);

    const contexto = await contextoLotes(usuarioId);
    const contents = [
      ...historial
        .filter((m: Record<string, unknown>) => typeof m?.texto === "string" && m.texto.length > 0)
        .slice(-8)
        .map((m: Record<string, unknown>) => ({
          role: m.deUsuario ? "user" : "model",
          parts: [{ text: m.texto }],
        })),
      { role: "user", parts: [{ text: mensaje }] },
    ];

    const r = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/${MODELO}:generateContent`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json", "x-goog-api-key": claveProveedor },
        body: JSON.stringify({
          systemInstruction: { parts: [{ text: PROMPT_SISTEMA + contexto }] },
          contents,
          generationConfig: { temperature: 0.3, maxOutputTokens: 400 },
        }),
      },
    );

    if (r.status === 429) return json({ error: "Demasiadas consultas seguidas, intenta en un momento." }, 429);
    if (!r.ok) {
      console.error("gemini", r.status, await r.text());
      return json({ error: r.status === 400 ? "El modelo del asesor no está disponible." : "El asesor no está disponible." }, 502);
    }

    const data = await r.json();
    const texto: string = (data?.candidates?.[0]?.content?.parts ?? [])
      .map((p: Record<string, unknown>) => (typeof p?.text === "string" ? p.text : ""))
      .join("")
      .trim();

    if (!texto) return json({ error: "El asesor no devolvió respuesta." }, 502);
    return json({ texto });
  } catch (e) {
    console.error("asesor-ia", e);
    return json({ error: "Servicio no disponible." }, 500);
  }
});
