# Consigue tu clave gratis de la API de Gemini

*[Read in English](API_KEY_GUIDE.md)*

ConceptGames usa la IA **Gemini** de Google para convertir un tema en juegos de estudio. Gemini tiene un **nivel gratuito**, así que cada jugador usa su propia clave. Así no hay servidor que mantener, ni una cuota compartida que se agote, ni cuentas que crear en la app.

Toma unos dos minutos y no necesitas tarjeta de crédito.

## Pasos

1. **Abre Google AI Studio.** Ve a **<https://aistudio.google.com/apikey>** e inicia sesión con cualquier cuenta de Google. En la app, el botón *Abrir Google AI Studio* te lleva directo.
2. **Crea una clave de API.** Toca **Create API key** (Crear clave de API). Si te pide elegir un proyecto de Google Cloud, el predeterminado sirve, o puedes crear uno nuevo. AI Studio activa la API de Gemini en ese proyecto automáticamente.
3. **Copia la clave.** Es un código largo que normalmente empieza con `AIza`. Usa el botón de copiar que está a su lado.
4. **Pégala en ConceptGames.** Vuelve a la app, toca **Pegar** y luego **Probar y guardar**. La app hace una solicitud gratuita (la lista de modelos) para confirmar que Google acepta la clave, y solo entonces la guarda.

   ✅ *"¡Tu clave funciona!"* significa que ya puedes crear juegos.

Puedes volver a abrir esta guía cuando quieras desde **Ajustes → IA → Gestionar**.

## Conviene saber

| | |
|---|---|
| 🔒 **Mantenla en privado** | Cualquiera con tu clave puede usar tu cuota. No la compartas, no la subas a git y no la publiques. |
| 💸 **Sigue gratis** | No actives la facturación del proyecto a menos que quieras pagar. Sin facturación, Google simplemente rechaza solicitudes al llegar al límite gratuito; nunca se te cobra. |
| 🛡️ **Qué se envía** | El tema, el texto de Wikipedia que elegiste y la instrucción se envían a la API de Gemini. En el nivel gratuito Google puede usar este contenido para mejorar sus productos ([términos de la API de Gemini](https://ai.google.dev/gemini-api/terms)). No escribas información personal. |
| 📱 **Dónde se guarda** | La clave se cifra con una clave AES que vive en el Android Keystore de tu teléfono. Se excluye de las copias de seguridad y solo se envía a `generativelanguage.googleapis.com`. |
| ♻️ **Revocar o cambiar** | Borra la clave en AI Studio cuando quieras, crea otra y pégala en la app. *Ajustes → Gestionar → Eliminar la clave guardada* la borra del teléfono. |
| 🔎 **Búsqueda web** | ConceptGames puede agregar un paso de Búsqueda de Google ("grounding") además de Wikipedia. Google no incluye el grounding en el nivel gratuito de sus modelos más nuevos, así que con una clave gratuita este paso normalmente se omite y la pantalla explica por qué. Tus juegos se crean igual con los artículos de Wikipedia que elegiste. Puedes desactivar el paso en *Ajustes*. |
| ⏳ **Límites** | Las claves gratuitas tienen límites por minuto y por día que Google cambia de vez en cuando. Consulta tus límites actuales en [AI Studio](https://aistudio.google.com/usage). Crear un tema cuesta 1 solicitud, o 2 con la búsqueda web activada. Volver a jugar no cuesta nada. |

## Solución de problemas

| Mensaje en la app | Qué significa | Qué hacer |
|---|---|---|
| **Esa clave no funciona** (HTTP 400 `API_KEY_INVALID` o 401/403) | La clave está mal escrita, incompleta o fue borrada. | Cópiala de nuevo desde AI Studio, sin espacios ni caracteres faltantes. |
| **La API de Gemini está desactivada** (403 `SERVICE_DISABLED`) | La clave pertenece a un proyecto donde la Generative Language API no está activa. | Crea la clave desde AI Studio, que activa la API, o actívala en la consola de Google Cloud. |
| **No disponible en tu región** (400 `FAILED_PRECONDITION`) | La API de Gemini no se ofrece donde estás. | Consulta las [regiones disponibles](https://ai.google.dev/gemini-api/docs/available-regions). |
| **Cuota gratuita agotada** (429 `RESOURCE_EXHAUSTED`) | Llegaste a un límite gratuito por minuto o por día. | Espera un minuto o vuelve mañana. También puedes elegir un modelo más ligero (p. ej. `gemini-flash-lite-latest`) en *Ajustes*. |
| **Modelo no disponible** (404) | El modelo elegido en *Ajustes* no existe para tu clave. | Elige `gemini-flash-latest` u otro modelo de la lista. |
| **Gemini está ocupado** (500/503) | Los servidores de Google están saturados. | La app reintenta automáticamente. Si sigue fallando, inténtalo en un rato. |

## Para desarrolladores

Las compilaciones debug pueden precargar una clave desde `local.properties` (ignorado por git):

```properties
gemini.apiKey=AIza...
```

Nunca subas una clave real. La CI no necesita ninguna, porque las pruebas usan respuestas simuladas.
