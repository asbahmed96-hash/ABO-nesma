# ABO NESMA 🦅
Offline Android study assistant: PDF -> chapters/lessons -> summaries, formulas, definitions, comparisons, flashcards, question bank, exams, error review, TXT/JSON export. No internet, accounts, cloud or AI models.

## Build the APK on GitHub (no Android Studio needed)
1. Create an empty GitHub repo and push this folder:
   git init && git add . && git commit -m "ABO NESMA" && git branch -M main && git remote add origin <URL> && git push -u origin main
2. Open the repo -> Actions -> "Android CI" (runs on every push, or "Run workflow").
3. When green, open the run and download artifact **ABO-NESMA-debug-apk**.
The workflow installs JDK 17, Android SDK and Gradle 8.9 itself (no wrapper jar is bundled because it could not be downloaded here).

## IMPORTANT status
Authored without Android SDK/Gradle/network: the code has NOT been compiled and the unit tests have NOT been run. The first CI run may need small fixes; send the log.

## OCR for scanned PDFs (offline)
Put `ara.traineddata` and `eng.traineddata` (tessdata_fast from github.com/tesseract-ocr/tessdata_fast) in `app/src/main/assets/tessdata/`. Without them, scanned PDFs show a clear message.

## Known limits
- Extraction is rule-based (regex), so quality depends on how the book is written; tables, complex equations and symbol meanings are not parsed.
- PDFBox may return Arabic text in visual order for some PDFs.
- Questions come only from extracted definitions; thin chapters give fewer questions plus a note (never invented).
- Essay questions are shown with a reference answer only (no automatic grading).
- Translation is a dictionary of ~120 terms (assets/dict_en_ar.tsv, extend it freely): word/phrase level, not professional.
- Summaries/definitions/cards/questions are rebuilt from the saved lesson text on demand (deterministic); books, chapters, lessons, attempts, wrong answers and card mastery are stored in Room.
