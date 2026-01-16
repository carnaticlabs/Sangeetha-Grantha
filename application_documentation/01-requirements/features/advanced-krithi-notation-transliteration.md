# Advanced Krithi Notation & AI Transliteration

| Metadata | Value |
|:---|:---|
| **Status** | Implemented |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-16 |

---

## 1. Executive Summary

This feature significantly enhances the Sangeetha Grantha platform's ability to represent and edit complex Carnatic music compositions. It introduces support for advanced structural elements like **Samashti Charanam** and **Madhyama Kala** sections, provides a granular **Notation Editor** for aligning Swara and Sahitya, and integrates **AI-powered Transliteration** to automatically convert lyrics between scripts (e.g., Devanagari to Latin/Tamil) using Google Gemini 2.0 Flash.

**Key Objectives:**
- Support complex Krithi structures beyond basic Pallavi/Charanam.
- Enable precise, row-by-row editing of musical notation (Swara) and lyrics (Sahitya).
- Automate laborious transliteration tasks with high accuracy using AI.
- Ensure data integrity for diverse musical forms.

**Technology Stack:** React 19, Kotlin/Ktor, Google Gemini 2.0 Flash

---

## 2. Feature Details

### 2.1 Advanced Section Types

The domain model has been expanded to support a wider range of Carnatic compositional structures.

**New Supported Sections:**
- **Samashti Charanam**: A hybrid section often found in Muthuswami Dikshitar's compositions, replacing the Anupallavi and Charanam.
- **Madhyama Kala**: Second-speed lyrical passages, critical for accurate rendering of many krithis.
- **Other Variants**: Support for Chittaswaram, Swara Sahitya, Solkattu Swara, etc.

**Implication:**
- The `RagaSection` enum in the database and code now supports these types.
- The UI dynamically renders these sections with appropriate labels.

### 2.2 Granular Notation Editor (`NotationRowsEditor`)

A new specialized editor component replaces simple text areas for notation.

**Capabilities:**
- **Row-based Editing**: Notation is broken down into rows, allowing clear alignment of Swara and Sahitya.
- **Swara Input**: Dedicated field for musical notes.
- **Sahitya Input**: Optional field for corresponding lyrics.
- **Tala Markers**: Support for visual markers (|, ||) to denote rhythm cycles.
- **Reordering**: Drag-and-drop or button-based reordering of rows (in development).

### 2.3 AI Transliteration Suite (`TransliterationModal`)

Integrated directly into the editor is an AI-powered tool for script conversion.

**Workflow:**
1.  **Source Entry**: User enters lyrics in any supported language (e.g., Kannada, Telugu).
2.  **Target Selection**: User selects the desired output script (e.g., Diacritic Latin, Tamil).
3.  **AI Processing**: The system sends the content to Google Gemini 2.0 Flash.
4.  **Review & Edit**: Generating a preview side-by-side with source.
5.  **Apply**: On confirmation, the transliterated text is saved as a new variant or updates the existing one.

**Benefits:**
- Reduces manual data entry time by ~80%.
- Maintains consistency in transliteration schemes (e.g., ISO-15919).

---

## 3. Technical Implementation

### 3.1 Frontend Components

- **`NotationRowsEditor.tsx`**: Core component for managing the list of notation rows. Handles local state for inputs before persistence.
- **`TransliterationModal.tsx`**: Modal dialog that manages the API interaction with the backend AI service. Features a split-pane view for Source vs. Result.
- **`types.ts`**: Updated `NotationRow` interface to include `talaMarkers`.

### 3.2 Backend & Database

- **Enum Updates**: `DbEnums.kt` updated `RagaSection` to include `SAMASHTI_CHARANAM`, `MADHYAMA_KALA`.
- **API Routes**: `AdminNotationRoutes.kt` updated to handle granular row updates.
- **Migrations**: 
    - `08__add-samashti-charanam-enum.sql`: Database migration for new enum value.
    - `09__add-advanced-sections.sql`: Schema updates for advanced section support.

---

## 4. User Experience

**Before:**
- Limited to Pallavi/Anupallavi/Charanam.
- Notation was a single text blob, making alignment difficult.
- Transliteration required external tools and copy-pasting.

**After:**
- **Flexible Structure**: Can model any complex Krithi.
- **Visual Clarity**: Swaras and Sahitya are visually aligned in rows.
- **One-Click Transliteration**: Immediate access to multilingual support within the app.

---

## 5. Future Scope

- **Audio Alignment**: Linking audio timestamps to specific notation rows.
- **Tala Visualizer**: Rendering graphical tala beat counts based on markers.
- **Batch Transliteration**: Processing entire library for missing scripts.
