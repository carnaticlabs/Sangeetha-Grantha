import { describe, it, expect } from 'vitest';
import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { DTO_FIELDS } from './mobilePreviewDtoFields';

const CATALOGUE_DIR = join(process.cwd(), '../../shared/domain/src/commonMain/kotlin/com/sangita/grantha/shared/domain/model/catalogue');

/** Property names of `data class <name>(...)`: the `val` declarations in its primary constructor. */
const kotlinFields = (source: string, name: string): string[] | null => {
    const start = source.search(new RegExp(`data class ${name}\\s*(<[^>]*>)?\\s*\\(`));
    if (start < 0) return null;
    const open = source.indexOf('(', start);
    let depth = 0;
    let end = open;
    for (let i = open; i < source.length; i += 1) {
        if (source[i] === '(') depth += 1;
        if (source[i] === ')') { depth -= 1; if (depth === 0) { end = i; break; } }
    }
    const body = source.slice(open + 1, end);
    return [...body.matchAll(/\bval\s+(\w+)\s*:/g)].map((m) => m[1]);
};

const allKotlin = (): string => readdirSync(CATALOGUE_DIR).filter((f) => f.endsWith('.kt')).map((f) => readFileSync(join(CATALOGUE_DIR, f), 'utf8')).join('\n');

describe('Kotlin DTOs and the TypeScript mirror stay in step', () => {
    const source = allKotlin();
    it.each(Object.entries(DTO_FIELDS))('%s', (name, listed) => {
        const actual = kotlinFields(source, name);
        expect(actual, `data class ${name} not found under ${CATALOGUE_DIR}`).not.toBeNull();
        expect([...(actual ?? [])].sort()).toEqual([...listed].sort());
    });
});
