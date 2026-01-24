| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-24 |
| **Author** | Sangeetha Grantha Team |

# Feature: Searchable Deity and Temple Management with Modal Forms

| Metadata | Value |
|:---|:---|
| **Status** | Implemented |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-20 |

---

## 1. Executive Summary

This feature enhances the user experience for managing Deity and Temple entities throughout the Sangeetha Grantha admin interface. The implementation introduces searchable select fields, modal-based create/edit forms, and comprehensive Deity entity management capabilities. These improvements streamline the workflow for content curators when working with compositions that reference deities and temples, eliminating the need to navigate away from the composition editor to manage reference data.

**Key Objectives:**
- Enable searchable selection of Deities and Temples in composition forms
- Provide quick access to create/edit Deity and Temple entities via modal popups
- Complete the Deity entity management functionality (previously only listing was available)
- Improve discoverability and usability of reference data management
- Maintain consistency with existing entity management patterns

**Technology Stack:** React 19 + TypeScript (frontend), Kotlin + Ktor (backend), PostgreSQL (database)

---

## 2. Targeted User Personas

**Primary Users:** Content curators and administrators working with Krithi compositions in the Sangeetha Grantha admin portal.

**User Stories:**
- As a **content curator**, I want to search for deities when editing a composition so that I can quickly find the correct deity without scrolling through long dropdown lists.
- As a **content curator**, I want to create a new deity directly from the composition editor so that I don't have to navigate away and lose my editing context.
- As a **content curator**, I want to update deity information from the composition editor so that I can fix typos or add descriptions without interrupting my workflow.
- As an **administrator**, I want to manage deities from the Reference Data page so that I can maintain the canonical deity registry.
- As a **content curator**, I want the same searchable and modal capabilities for temples so that I have a consistent experience across all reference entities.

---

## 3. Functional Requirements

### 3.1 Searchable Select Field Component (High Priority)

**Requirement:** Replace standard HTML select dropdowns with a searchable, keyboard-navigable select component for Deity and Temple selection.

**Details:**
- **Search Functionality:** Real-time filtering of options as user types
- **Keyboard Navigation:** Support for Arrow Up/Down, Enter, and Escape keys
- **Visual Feedback:** Highlight focused option, show selected state
- **Accessibility:** Proper ARIA attributes for screen readers
- **Add New Option:** Prominent "Add New" button at bottom of dropdown
- **Performance:** Handle lists of 100+ items with smooth scrolling and filtering

**Acceptance Criteria:**
- User can type to filter options in real-time
- User can navigate options using keyboard
- Selected value is clearly displayed
- "Add New" button opens appropriate modal

### 3.2 Deity Modal Form (High Priority)

**Requirement:** Provide a modal dialog for creating and editing Deity entities.

**Details:**
- **Form Fields:**
  - Name (required, text input)
  - Normalized Name (optional, text input with auto-generation hint)
  - Description (optional, textarea)
- **Validation:** Name field is required before submission
- **Create Mode:** Empty form for new deity creation
- **Edit Mode:** Pre-populated form when editing existing deity
- **Success Handling:** Refresh deity list and update composition form selection
- **Error Handling:** Display user-friendly error messages

**Acceptance Criteria:**
- Modal opens when "Add New Deity" is clicked from select field
- Form validates required fields
- Success message displayed on save
- Deity list refreshes and new deity is selectable
- Modal closes after successful save

### 3.3 Temple Modal Form (High Priority)

**Requirement:** Provide a modal dialog for creating and editing Temple entities.

**Details:**
- **Form Fields:**
  - Name (required, text input)
  - Normalized Name (optional, text input)
  - City, State, Country (optional, text inputs)
  - Primary Deity (optional, select dropdown with deity list)
  - Latitude, Longitude (optional, number inputs)
  - Notes (optional, textarea)
- **Deity Selection:** Load and display available deities in primary deity dropdown
- **Validation:** Name field is required before submission
- **Create/Edit Modes:** Support both creation and editing workflows
- **Success Handling:** Refresh temple list and update composition form selection

**Acceptance Criteria:**
- Modal opens when "Add New Temple" is clicked from select field
- All form fields are functional
- Primary deity dropdown loads available deities
- Success message displayed on save
- Temple list refreshes and new temple is selectable

### 3.4 Deity Entity Management in Reference Data (High Priority)

**Requirement:** Add complete CRUD functionality for Deity entities in the Reference Data management page.

**Details:**
- **List View:** Display all deities in a table with name, normalized name, and description columns
- **Create Form:** Full-page form for creating new deities
- **Edit Form:** Full-page form for editing existing deities
- **Delete Functionality:** Support deletion of deity entities (with appropriate safeguards)
- **Home Card:** Add Deity card to Reference Data home page showing count and description
- **Consistency:** Follow same patterns as Composers, Ragas, Talas, and Temples

**Acceptance Criteria:**
- Deity card appears on Reference Data home page
- Clicking Deity card navigates to deity list
- Create button opens deity creation form
- Edit button opens deity editing form
- Deity list displays all deities with proper formatting
- Delete functionality works (if implemented)

### 3.5 Backend API Support for Deity CRUD (High Priority)

**Requirement:** Implement complete backend API endpoints for Deity entity management.

**Details:**
- **Repository Layer:** Add `findById`, `update`, and `delete` methods to `DeityRepository`
- **Service Layer:** Add `getDeity`, `createDeity`, `updateDeity`, and `deleteDeity` methods
- **API Routes:** Implement REST endpoints at `/v1/admin/deities`
  - `GET /v1/admin/deities` - List all deities
  - `GET /v1/admin/deities/{id}` - Get single deity
  - `POST /v1/admin/deities` - Create deity
  - `PUT /v1/admin/deities/{id}` - Update deity
  - `DELETE /v1/admin/deities/{id}` - Delete deity
- **Request Models:** Create `DeityCreateRequest` and `DeityUpdateRequest` DTOs
- **Audit Logging:** Log all create, update, and delete operations

**Acceptance Criteria:**
- All CRUD endpoints are functional
- Request/response models match frontend expectations
- Audit logs are created for mutations
- Error handling returns appropriate HTTP status codes

### 3.6 Integration with Composition Editor (High Priority)

**Requirement:** Integrate searchable selects and modals into the KrithiEditor component.

**Details:**
- **Replace SelectFields:** Update Deity and Temple fields to use `SearchableSelectField`
- **Modal Integration:** Add DeityModal and TempleModal components to editor
- **State Management:** Handle modal open/close states
- **Data Refresh:** Reload deity/temple lists after modal saves
- **Selection Update:** Automatically select newly created entity in form

**Acceptance Criteria:**
- Deity and Temple fields are searchable
- "Add New" buttons open appropriate modals
- Newly created entities are automatically selected
- Lists refresh after modal saves

### 3.7 Dashboard Integration (Medium Priority)

**Requirement:** Update Dashboard to reflect Deity management capabilities.

**Details:**
- **Reference Library Card:** Update description to mention Deities
- **Consistency:** Ensure all reference entities are mentioned

**Acceptance Criteria:**
- Dashboard Reference Library card mentions Deities
- Link to Reference Data page is functional

---

## 4. Technical Implementation

### 4.1 Frontend Components

**New Components:**
- `SearchableSelectField.tsx` - Reusable searchable select component
- `DeityModal.tsx` - Modal form for deity create/edit
- `TempleModal.tsx` - Modal form for temple create/edit

**Modified Components:**
- `KrithiEditor.tsx` - Integrated searchable selects and modals
- `ReferenceData.tsx` - Added Deity entity management
- `Dashboard.tsx` - Updated Reference Library description

### 4.2 Backend Changes

**Repository Layer:**
- `DeityRepository.kt` - Added `findById`, `update`, and `delete` methods
- Normalization helper function for name processing

**Service Layer:**
- `ReferenceDataService.kt` - Added Deity CRUD service methods
- Audit logging integration

**API Layer:**
- `ReferenceDataRoutes.kt` - Added `/v1/admin/deities` route handlers
- `ReferenceDataRequests.kt` - Added `DeityCreateRequest` and `DeityUpdateRequest`

### 4.3 API Client

**New Functions:**
- `getDeity(id: string)` - Fetch single deity
- `createDeity(payload)` - Create new deity
- `updateDeity(id, payload)` - Update existing deity
- `deleteDeity(id: string)` - Delete deity

### 4.4 Database

**No Schema Changes Required:**
- Existing `deities` table structure supports all operations
- All required fields (id, name, name_normalized, description, timestamps) are present

---

## 5. User Experience Improvements

### 5.1 Before Implementation

**Pain Points:**
- Standard HTML select dropdowns required scrolling through long lists
- No way to create deities/temples from composition editor
- Deity management was incomplete (only listing available)
- Inconsistent experience between different reference entities

### 5.2 After Implementation

**Improvements:**
- ✅ Searchable dropdowns with real-time filtering
- ✅ Quick access to create/edit via modals without navigation
- ✅ Complete Deity CRUD functionality
- ✅ Consistent patterns across all reference entities
- ✅ Keyboard navigation support for accessibility
- ✅ Automatic selection of newly created entities

---

## 6. Testing Considerations

### 6.1 Unit Testing

- SearchableSelectField component rendering and interaction
- Modal form validation and submission
- API client function error handling

### 6.2 Integration Testing

- Modal save flow with list refresh
- Composition editor integration
- Reference Data page deity management

### 6.3 User Acceptance Testing

- Search functionality with large lists (100+ items)
- Keyboard navigation in select fields
- Modal workflows for create and edit
- Reference Data page deity management

---

## 7. Future Enhancements

### 7.1 Potential Improvements

- **Bulk Operations:** Support bulk import/export of deities
- **Advanced Search:** Filter by description, related temples, etc.
- **Relationship Visualization:** Show which temples reference a deity
- **Validation Rules:** Prevent deletion of deities referenced by temples
- **History Tracking:** Show edit history for deities
- **Duplicate Detection:** Warn when creating deities with similar names

### 7.2 Related Features

- Extend searchable select pattern to other reference entities (Ragas, Talas)
- Add similar modal workflows for Composers
- Implement relationship management (e.g., temple-deity associations)

---

## 8. Dependencies

### 8.1 Frontend Dependencies

- React 19.2.3 (existing)
- React Router DOM 7.11.0 (existing)
- TypeScript 5.8.2 (existing)
- No new external dependencies required

### 8.2 Backend Dependencies

- Kotlin 2.2.20 (existing)
- Ktor 3.3.1 (existing)
- Exposed 1.0.0-rc-2 (existing)
- No new dependencies required

---

## 9. Migration Notes

### 9.1 Data Migration

**No data migration required** - All existing deity records are compatible with new functionality.

### 9.2 Code Migration

**Breaking Changes:** None

**Deprecations:** None

**New Patterns:**
- `SearchableSelectField` component can be reused for other entity selections
- Modal pattern established for quick entity creation/editing

---

## 10. References

### 10.1 Related Documentation

- [Database Schema](../../04-database/schema.md) - Deity and Temple table structures
- [UI to API Mapping](../../03-api/ui-to-api-mapping.md) - API endpoint specifications
- [Admin Web UI Specs](../../05-frontend/admin-web/ui-specs.md) - Admin web interface patterns

### 10.2 Implementation Files

**Frontend:**
- `modules/frontend/sangita-admin-web/src/components/SearchableSelectField.tsx`
- `modules/frontend/sangita-admin-web/src/components/DeityModal.tsx`
- `modules/frontend/sangita-admin-web/src/components/TempleModal.tsx`
- `modules/frontend/sangita-admin-web/src/pages/KrithiEditor.tsx`
- `modules/frontend/sangita-admin-web/src/pages/ReferenceData.tsx`
- `modules/frontend/sangita-admin-web/src/pages/Dashboard.tsx`
- `modules/frontend/sangita-admin-web/src/api/client.ts`

**Backend:**
- `modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/DeityRepository.kt`
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/ReferenceDataService.kt`
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/ReferenceDataRoutes.kt`
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/models/ReferenceDataRequests.kt`

---

## 11. Change Log

### Version 1.0.0 (2025-01-27)

**Initial Implementation:**
- ✅ Created SearchableSelectField component
- ✅ Implemented DeityModal and TempleModal components
- ✅ Added complete Deity CRUD backend support
- ✅ Integrated searchable selects into KrithiEditor
- ✅ Added Deity management to ReferenceData page
- ✅ Updated Dashboard Reference Library description
- ✅ Added frontend API client functions for Deity operations

---

## 12. Approval and Sign-off

**Status:** ✅ Implemented and Ready for Review

**Next Steps:**
1. Code review and testing
2. User acceptance testing with content curators
3. Documentation updates (if needed)
4. Deployment to staging environment

---

*This document follows the Sangeetha Grantha feature documentation standards and should be updated as the feature evolves.*
