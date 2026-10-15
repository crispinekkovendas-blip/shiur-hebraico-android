# Shiur Hebraico — Android

The native app of https://shiur-hebraico.vercel.app (Kotlin + Compose). The website is the backend:
`/app/catalogo/` (curated lessons, teachers, parashiot, this week's parashá), `/estudo/<id>.json`
(summary, moments, questions, sources) and `/legendas/?v=<id>` (Hebrew cues + pt + en).

Builds only on GitHub: every push to `main` → APK artifact + release `latest`.
