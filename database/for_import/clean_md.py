import re

def clean_markdown_file(input_path, output_path):
    with open(input_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Define patterns to remove
    patterns_to_remove = [
        # Match image hallucination texts entirely
        r"(?i)##?\\s*This image contains no text.*?\n",
        r"(?i)##?\\s*(This|The) image contains no text.*?(\n|$)",
        r"(?i)(This|The) (provided )?image contains no text.*?(\n|$)",
        r"(?i)(This|The) (provided )?image contains.*?(\n|$)",
        r"(?i)(This|The) image displays.*?(\n|$)",
        r"(?i)(This|The) image shows.*?(\n|$)",
        
        # Match the specific math blocks
        r"\$\$.*?\\mathbf\{K\}.*?\$\$",
        r"\$\$.*?\$\$", # Any other math block hallucination if any? Maybe too broad, let's just do \mathbf pattern first
        r"\\mathbf\{[^\}]+\}",

        # Match block characters
        r"█+",
        r"■+",
        r"⬛+",
        
        # Other common Sarvam vision artifacts
        r"\[data-radix-scroll-area-viewport\].*?\{.*?\}",
    ]

    cleaned_content = content
    for pattern in patterns_to_remove:
        cleaned_content = re.sub(pattern, "", cleaned_content)

    # Now filter line by line to remove lines that don't contain any Devanagari or useful markdown structure?
    # Actually, user said "retain only sanskrit characters related to Krithi". 
    # Let's remove lines that are purely English text without any Devanagari, if they look like hallucinations.
    # But wait, english headers might exist. Let's stick to the specific noise classes first + pure ascii garbage.

    lines = cleaned_content.split('\n')
    final_lines = []
    
    def has_devanagari(text):
        # Devanagari unicode range is 0900–097F
        return bool(re.search(r'[\u0900-\u097F]', text))

    for line in lines:
        s = line.strip()
        if not s:
            final_lines.append(line)
            continue
            
        # If it's pure math or symbolic garbage remaining
        if s.startswith('$$') and s.endswith('$$'):
            continue
            
        # Sarvam sometimes outputs sequence of identical symbols
        if set(s) == {'K'} or set(s) == {'X'} or set(s) == {'I'}:
            continue
            
        final_lines.append(line)

    # Write cleaned content
    with open(output_path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(final_lines))

if __name__ == "__main__":
    import sys
    clean_markdown_file(sys.argv[1], sys.argv[2])
