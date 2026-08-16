---
week: 2
theme: i18n
date: 2026-07-18
title: "Internacionalización (i18n)"
summary: "Cómo preparar una aplicación para hablar varios idiomas: archivos de traducción, detección de idioma y textos dinámicos sin duplicar código."
---

Esta semana el curso nos llevó a un problema muy real: nuestra aplicación solo hablaba español,
pero los usuarios no. La **internacionalización** —abreviada *i18n*, porque hay 18 letras entre
la "i" y la "n"— es la práctica de diseñar el software para que soporte varios idiomas sin
reescribir el código.

## ¿Qué problema resuelve?

Sin i18n, cada texto está quemado en el código. Si el botón dice `Iniciar sesión` y queremos la
versión en inglés, tocaría duplicar la pantalla completa: dos archivos que hacen lo mismo y que a
partir de ese día hay que corregir dos veces.

Con i18n el código no contiene frases sino **claves**, y las frases viven en archivos de
traducción, uno por idioma.

```json
// messages/es.json
{ "nav.login": "Iniciar sesión" }

// messages/en.json
{ "nav.login": "Sign in" }
```

```tsx
// En la vista, en lugar del texto va la clave:
<span>{t["nav.login"]}</span>
```

La pantalla queda una sola. Agregar un idioma pasa a ser agregar un archivo.

## Conceptos clave que vimos

- **Archivos de recursos**: un JSON —o un `.properties`, según la plataforma— por idioma, con
  pares clave–texto.
- **Detección de idioma**: puede venir del navegador, de la URL o de la preferencia guardada del
  usuario. Cada opción tiene consecuencias distintas y conviene elegir a conciencia.
- **Interpolación**: las traducciones aceptan variables, por ejemplo `"Hola, {nombre}"`. El orden
  de las palabras cambia entre idiomas, así que concatenar trozos sueltos no sirve.
- **Localización (l10n)**: va más allá del texto. Fechas, monedas, números y hasta el orden
  alfabético cambian según la región.

## Cómo lo aplicamos en el proyecto

El idioma vive **en la URL**: `/es`, `/en` y `/pt`. Es una decisión, no un detalle. Una dirección
con el idioma dentro se puede compartir, se puede guardar en marcadores y el botón "atrás"
funciona; si el idioma viviera solo en la memoria del navegador, dos personas abriendo el mismo
enlace verían cosas distintas.

Los tres diccionarios son archivos planos, y el español es el de referencia:

```ts
// El bundle español tipa a los otros dos: si en.json o pt.json
// pierden una clave, el proyecto no compila.
export type Dictionary = typeof es;
export type MessageKey = keyof Dictionary;
```

Ese detalle es el que más nos ahorró. Una traducción incompleta deja de ser algo que alguien
descubre en producción y pasa a ser un error de compilación.

De la localización se encarga el propio navegador a través de `Intl`, sin biblioteca extra:

```ts
new Intl.DateTimeFormat(locale, { day: "2-digit", month: "short", year: "numeric" });
new Intl.NumberFormat(locale, { style: "currency", currency: "USD" });
```

La misma fecha se lee `18 jul 2026` en español y `Jul 18, 2026` en inglés; el mismo precio se
escribe `$1,999` o `1999 US$` según dónde ponga cada idioma el símbolo.

En el backend tomamos el camino contrario, y también a propósito: **la API no traduce**. Cuando
algo falla responde un código estable —`BAD_CREDENTIALS`, `EMAIL_TAKEN`— y es el front quien lo
convierte en una frase. Así el idioma se decide en un solo lugar, el que tiene el diccionario. La
única excepción son los correos: el de recuperación de contraseña lo escribe y lo envía el
backend, así que allí sí viven sus tres versiones, y quien pide el enlace dice en qué idioma lo
quiere.

> **Lección aprendida:** conviene definir las claves desde el primer día. Migrar textos quemados a
> claves después es trabajo doble, y el momento en que duele es justo cuando ya hay pantallas
> suficientes para que dé pereza.

## Próximos pasos

Extender las traducciones a la zona privada y hacer que esta misma bitácora deje de ser estática:
las entradas pasarán a la base de datos con una fila por idioma, de modo que el contenido —no
solo los botones— viaje traducido.
