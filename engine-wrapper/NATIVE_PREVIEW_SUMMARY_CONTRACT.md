# Native Preview Summary Contract

The native Orca wrapper appends preview metadata to the pipe-separated slice summary returned to Android.
Android parses this in `GcodeSummaryParser.fromNativeSummary`.

## Field Encoding

- Top-level summary fields are separated by `|`.
- Each field is `key=value`.
- Preview table rows are separated by `;`.
- Preview row columns are separated by `,`.
- Native string columns are escaped with `summary_escape`.
- `summary_escape` leaves ASCII letters, digits, spaces, `-`, `_`, `.`, and `#` unchanged.
- Every other byte is encoded as uppercase `%XX`.
- Android must decode `%XX` sequences as UTF-8 bytes, not as individual UTF-16 characters.

## Preview Line Types

Key: `previewLineTypes`

Row format:

```text
kind,nativeId,label,colorHex,timeSeconds,percent,usageMeters,usageGrams,defaultVisible
```

- `kind` is `role` or `option`.
- `defaultVisible` is optional for older native summaries.
- Unknown kinds, invalid IDs, and partial rows are ignored.

## Preview Filaments

Key: `previewFilaments`

Row format:

```text
slotIndex,label,colorHex,modelMeters,modelGrams,supportMeters,supportGrams,flushedMeters,flushedGrams,towerMeters,towerGrams,totalMeters,totalGrams,cost
```

- Partial rows are ignored.
- Blank labels fall back to `Filament <slotIndex>`.
- Invalid colors fall back to `#8FC1FF`.

## Preview Totals

Key: `previewTotals`

Format:

```text
totalSeconds=<seconds>,prepareSeconds=<seconds>,modelSeconds=<seconds>,cost=<amount>,filamentChanges=<count>,extruderChanges=<count>
```

- Missing totals are valid and should leave nullable timing/cost fields unset.
- Invalid counts default to zero.
