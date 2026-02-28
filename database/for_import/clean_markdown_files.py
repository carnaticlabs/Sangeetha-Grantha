import re

def clean_file(in_path, out_path, lang):
    with open(in_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
        
    cleaned_lines = []
    
    for line in lines:
        stripped = line.strip()
        
        # 1. Remove thematic breaks which arbitrarily split krithi lyrics
        if stripped.startswith("---"):
            continue
            
        # 2. Remove footnote definitions at bottom of pages
        if stripped.startswith("[^"):
            continue
            
        # Remove inline footnotes like [^1]
        line = re.sub(r'\[\^[0-9]+\]', '', line)
        # Remove inline superscript numbers often used for footnotes
        line = re.sub(r'[\u00B9\u00B2\u00B3\u2070-\u2079]+', '', line)
        
        # 3. Standardize Raga/Tala labels to make them 100% deterministic for parsing
        if lang == 'eng':
            line = re.sub(r'(?i)r([aā]|a\u0304)ga([mṃ]|m\u0323)?\s*[:=]\s*', 'rāgam: ', line)
            line = re.sub(r'(?i)t([aā]|a\u0304)([lḷ]|l\u0323)a([mṃ]|m\u0323)?\s*[:=]\s*', 'tālam: ', line)
            
            # Remove "## " from headers to simplify title matching
            if line.startswith("## "):
                line = line[3:]
                
        elif lang == 'skt':
            line = re.sub(r'राग[ंम्]?\s*[:=]\s*', 'रागं: ', line)
            line = re.sub(r'ताळ[ंम्]?\s*[:=]\s*', 'ताळं: ', line)
            
            if line.startswith("## "):
                line = line[3:]

        # Clean trailing whitespace
        line = line.rstrip() + '\n'
        
        # 4. Skip pure digit lines (often lingering page numbers)
        if line.strip().isdigit() and len(line.strip()) <= 3:
            continue

        cleaned_lines.append(line)
        
    # Remove multiple sequential blank lines for easier manual reading
    final_text = "".join(cleaned_lines)
    final_text = re.sub(r'\n{3,}', '\n\n', final_text)

    with open(out_path, 'w', encoding='utf-8') as f:
        f.write(final_text)

print("Cleaning mdeng.md -> mdeng_clean.md")
clean_file('mdeng.md', 'mdeng_clean.md', 'eng')

print("Cleaning mdskt.md -> mdskt_clean.md")
clean_file('mdskt.md', 'mdskt_clean.md', 'skt')

print("Done! Baseline clean markdown files are ready.")
