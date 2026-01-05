# Feature: Graph Explorer

## 1. Executive Summary
The Graph Explorer provides a visual interface to explore connections between entities in the Sangeetha Grantha database, such as Composers, Ragas, and Krithis.

## 2. Requirements
- Visual graph representation
- Interactive node expansion
- filtering by entity type

## 3. Technical Approach
- Uses Postgres Recursive CTEs (as per ADR-005)
- Frontend: React Flow or similar library
