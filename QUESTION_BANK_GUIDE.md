# Question Bank Guide

The app no longer reads the old `quiz.xml` question bank. Questions are stored as CSV files under:

`app/src/main/assets/questions/`

## Current bank

`questions/algebra/general.csv` contains the 261 questions migrated from the original XML source.
Because the original source did not provide subject/category metadata, those migrated questions are marked:

- Subject: `Algebra`
- Category: `General`

No additional categorization was invented during migration.

## Add another subject

Create a folder, for example:

`questions/trigonometry/`

Then create a CSV file such as:

`questions/trigonometry/basic_trigonometry.csv`

The header must be:

```text
id,subject,category,question,choice_a,choice_b,choice_c,choice_d,answer,correct_feedback,incorrect_feedback
```

Each question is **one spreadsheet row**. `answer` is `A`, `B`, `C`, or `D`.

Example:

```csv
trig_001,Trigonometry,Basic Trigonometry,"What is sin(90°)?",0,1,-1,2,B,Correct!,Try again.
```

### Easiest workflow

You can edit the CSV with Excel, Google Sheets, LibreOffice Calc, or another spreadsheet program. Add one question per row, save/export as CSV UTF-8, and put the file in the appropriate `questions/<subject>/` folder.

You do **not** need to write XML.

## Categories

The app automatically discovers subjects and categories from the CSV files. You do not need to register a new subject/category in Java code.

For example:

```text
questions/
├── algebra/
│   ├── general.csv
│   ├── linear_equations.csv
│   └── quadratics.csv
├── trigonometry/
│   └── basic_trigonometry.csv
└── economics/
    ├── supply_demand.csv
    └── macroeconomics.csv
```

## Important

- Keep the header exactly as shown.
- Use exactly four choices.
- Set `answer` to A, B, C, or D.
- Put commas/newlines inside a cell only when your spreadsheet application quotes the CSV field correctly.
- The old XML is kept in `legacy_question_source/` only as a backup and is not loaded by the app.
