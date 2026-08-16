---
title: "Internationalization (i18n)"
summary: "How to prepare an application to speak several languages: translation files, language detection, and dynamic text without duplicating code."
---

This week the course walked us into a very real problem: our application only spoke Spanish, but
our users did not. **Internationalization** —shortened to *i18n*, because there are 18 letters
between the "i" and the "n"— is the practice of designing software so it can support several
languages without rewriting the code.

## What problem does it solve?

Without i18n, every piece of text is burned into the code. If the button says `Iniciar sesión`
and we want the English version, we would have to duplicate the whole screen: two files doing the
same job, and from that day on every fix has to be made twice.

With i18n the code holds no sentences, only **keys**, and the sentences live in translation
files, one per language.

```json
// messages/es.json
{ "nav.login": "Iniciar sesión" }

// messages/en.json
{ "nav.login": "Sign in" }
```

```tsx
// In the view, the key goes where the text used to be:
<span>{t["nav.login"]}</span>
```

There is one screen again. Adding a language becomes adding a file.

## The concepts we covered

- **Resource files**: one JSON —or a `.properties`, depending on the platform— per language,
  holding key–text pairs.
- **Language detection**: it can come from the browser, from the URL, or from a saved user
  preference. Each option has different consequences and deserves a deliberate choice.
- **Interpolation**: translations take variables, for example `"Hello, {name}"`. Word order
  changes between languages, so stitching fragments together does not work.
- **Localization (l10n)**: it goes beyond text. Dates, currencies, numbers and even alphabetical
  order change by region.

## How we applied it in the project

The language lives **in the URL**: `/es`, `/en` and `/pt`. That is a decision, not a detail. An
address with the language inside it can be shared, can be bookmarked, and the back button works;
if the language only lived in the browser's memory, two people opening the same link would see
different things.

The three dictionaries are flat files, and Spanish is the reference one:

```ts
// The Spanish bundle types the other two: if en.json or pt.json
// lose a key, the project does not compile.
export type Dictionary = typeof es;
export type MessageKey = keyof Dictionary;
```

That detail saved us the most. An incomplete translation stops being something someone discovers
in production and becomes a compile error.

Localization is handled by the browser itself through `Intl`, with no extra library:

```ts
new Intl.DateTimeFormat(locale, { day: "2-digit", month: "short", year: "numeric" });
new Intl.NumberFormat(locale, { style: "currency", currency: "USD" });
```

The same date reads `18 jul 2026` in Spanish and `Jul 18, 2026` in English; the same price is
written `$1,999` or `1999 US$` depending on where each language puts the symbol.

On the backend we took the opposite road, also on purpose: **the API does not translate**. When
something fails it answers with a stable code —`BAD_CREDENTIALS`, `EMAIL_TAKEN`— and the front is
what turns it into a sentence. That way the language is decided in a single place, the one that
owns the dictionary. The only exception is email: the password recovery message is written and
sent by the backend, so its three versions live there, and whoever requests the link says which
language they want it in.

> **Lesson learned:** define the keys from day one. Migrating hardcoded text to keys later is
> double work, and the moment it hurts is exactly when there are enough screens for the job to
> feel tedious.

## Next steps

Extend the translations to the private area, and make this very log stop being static: entries
will move to the database with one row per language, so the content —not just the buttons—
travels translated.
