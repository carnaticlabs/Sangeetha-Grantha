import re
import json
from pathlib import Path

def parse_mdskt(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # The krithis are generally prefixed by full devanagari numbers e.g., १, २
    # Or in the table of contents. Need to skip the table of contents.
    # The actual content starts around where the krithis have headings or text.
    # We can use the raga and tala line as a reliable anchor: `रागं : [raga] ताळं : [tala]`
    
    krithis = []
    
    # Split by double newlines or similar to process block by block
    blocks = content.split('\n\n')
    current_krithi = {}
    
    for block in blocks:
        block = block.strip()
        if not block:
            continue
            
        raga_match = re.search(r"रागं\s*:\s*([^\(]+)(?:\([0-9]+\))?\s*ताळं\s*:\s*(.+)", block)
        if raga_match:
            # Found a krithi metadata block
            if current_krithi:
                krithis.append(current_krithi)
                
            raga = raga_match.group(1).strip()
            tala = raga_match.group(2).strip()
            
            # The title is usually the line before this block, but splitting by double newline might separate them.
            # Let's try a different approach: line by line state machine.
            current_krithi = {'raga': raga, 'tala': tala, 'sections': []}
            
    # Wait, the block approach is flawed because title could be in a separate block.
    return krithis

def extract_krithis_state_machine(filepath, lang):
    with open(filepath, 'r', encoding='utf-8') as f:
        text = f.read()

    lines = text.split('\n')
        
    krithis = []
    
    # We want to skip everything before the first raga metadata is encountered,
    # to avoid pulling in the Table of Contents.
    found_first_krithi = False

    def extract_title_from_line(l, lng, expected_id):
        if lng == "eng":
            m = re.search(r"^\#*\s*([0-9]+)(?:\s+(.*))?$", l)
            if m and not re.search(r"\.\s*\.", l): # no TOC
                num = int(m.group(1))
                if abs(num - expected_id) < 5:
                    title = m.group(2).strip() if m.group(2) else "Unknown"
                    title = re.sub(r"\[\^[0-9]+\]:?\s.*?$", "", title).strip()
                    ret = title if title else "Unknown"
                    if expected_id <= 3:
                        print(f"[DEBUG EXTRACT] {lng} matched '{l}' as num {num}, returning '{ret}'")
                    return ret
        elif lng == "skt":
            m = re.search(r"^\#*\s*([\u0966-\u096F]+)(?:\s+(.*))?$", l)
            if m:
                dev_to_int = str.maketrans("०१२३४५६७८९", "0123456789")
                num = int(m.group(1).translate(dev_to_int))
                if abs(num - expected_id) < 5:
                    ret = m.group(2).strip() if m.group(2) else "Unknown"
                    if expected_id <= 3:
                        print(f"[DEBUG EXTRACT] {lng} matched '{l}' as num {num}, returning '{ret}'")
                    return ret
        return None

    if lang == "skt":
        raga_pattern = re.compile(r"राग[ंम्]?\s*[:=]\s*(.+?)(?:\s*\([0-9]+\))?(?:\s*ताळ[ंम्]?|$)")
        tala_pattern = re.compile(r"ताळ[ंम्]?\s*[:=]\s*(.+)")
        section_patterns = {
            'pallavi': re.compile(r"^\s*पल्लवि\s*$"),
            'anupallavi': re.compile(r"^\s*अनुपल्लवि\s*(\(समष्टिचरणम्\))?\s*$"),
            'charanam': re.compile(r"^\s*चरणम्\s*$"),
            'madhyamakala': re.compile(r"^\s*\([^\)]*मध्यमकालसाहित्यम्[^\)]*\)\s*$")
        }
    else:
        raga_pattern = re.compile(r"r(?:[aā]|a\u0304)ga(?:[mṃ]|m\u0323)?\s*[:=]\s*(.+?)(?:\s*\([0-9]+\))?(?:\s*t(?:[aā]|a\u0304)|$)", re.IGNORECASE)
        tala_pattern = re.compile(r"t(?:[aā]|a\u0304)(?:[lḷ]|l\u0323)a(?:[mṃ]|m\u0323)?\s*[:=]\s*(.+)", re.IGNORECASE)
        section_patterns = {
            'pallavi': re.compile(r"^\s*pallavi\s*$", re.IGNORECASE),
            'anupallavi': re.compile(r"^\s*anupallavi\s*(\([^\)]+\))?\s*$", re.IGNORECASE),
            'charanam': re.compile(r"^\s*cara[nṇ]a[mṃ]\s*$", re.IGNORECASE),
            'madhyamakala': re.compile(r"^\s*\([^\)]*madhyamak[aā]las[aā]hitya[mṃ][^\)]*\)\s*$", re.IGNORECASE)
        }

    current_krithi = None
    current_section = None
    buffer = []
    recent_titles = []
    
    for line in lines:
        line = line.strip()
        
        # Accumulate recent titles independently of the main buffer
        title_extract = extract_title_from_line(line, lang, len(krithis) + 1)
        if title_extract:
            recent_titles.append((title_extract, len(krithis) + 1))
            if len(recent_titles) > 10:
                recent_titles.pop(0)
        
        # Skip junk lines but allow plain numbers through in case they are title headers
        if not line or line.startswith("---") or line.startswith("## ") or "![" in line:
            if line.startswith("## ") and found_first_krithi:
                line = line[3:].strip()
            # If not found first Krithi, it might be the TOC header so skip
            elif not found_first_krithi:
                continue
                
        # Are we in TOC?
        # A simple check: if the line has ............ [page number], it is TOC
        if re.search(r"\.\s*\.\s*\.\s*[0-9]+$", line):
            continue

        raga_match = raga_pattern.search(line)
        tala_match = tala_pattern.search(line)
        
        # If we hit a raga match, it's definitively a new krithi
        if raga_match:
            found_first_krithi = True
            if current_krithi:
                if current_section:
                    current_krithi['sections'][current_section] = "\n".join(buffer).strip()
                krithis.append(current_krithi)

            current_krithi = {
                'id': len(krithis) + 1,
                'title': 'Unknown',
                'title_is_numbered': False,
                'raga': raga_match.group(1).strip(),
                'tala': tala_match.group(1).strip() if tala_match else 'Unknown',
                'sections': {}
            }
            if current_krithi['id'] <= 3:
                 print(f"[{lang}] Created Krithi {current_krithi['id']} with raga {current_krithi['raga']}")
            
            # 1. Pull from recent matched numbered titles (which survive buffer flushes)
            if recent_titles:
                current_krithi['title'] = recent_titles[-1][0]
                current_krithi['title_is_numbered'] = True
                if current_krithi['id'] <= 3:
                    print(f"[{lang}] Found title in recent_titles history: {current_krithi['title']}")
                recent_titles.clear()
            
            # 2. If no numbered title was found in the buffer, try finding a plain title from the end
            if not current_krithi['title_is_numbered']:
                for b in reversed(buffer):
                    is_sec = False
                    for sec_pattern in section_patterns.values():
                        if sec_pattern.search(b):
                            is_sec = True
                            break
                    # Avoid picking up long lyric strings or purely english footnotes
                    if b and not is_sec and len(b.split()) < 10 and not b.startswith("[^"):
                        current_krithi['title'] = b
                        if current_krithi['id'] <= 3:
                            print(f"[{lang}] Fallback title found: {b}")
                        break
                    
            if current_krithi['id'] <= 3:
                print(f"[{lang}] Final title for id {current_krithi['id']}: {current_krithi['title']}")
                
            current_section = None
            buffer = []
            continue

        if not current_krithi:
            # We are before the first raga match, so we shouldn't save junk to the buffer which ruins the first title
            if found_first_krithi:
                buffer.append(line)
            else:
                # Except we need the title of the very first krithi... which is right before the metadata
                # Just keep a rolling small buffer
                if len(buffer) > 5:
                    buffer.pop(0)
                buffer.append(line)
            continue
            
        # Catch a stray tala on the next line
        if tala_match and current_krithi['tala'] == 'Unknown':
             current_krithi['tala'] = tala_match.group(1).strip()
             continue
             
        # Only overwrite the title moving forward if it is Unknown and not found yet
        if current_krithi and not current_krithi['title_is_numbered']:
             extracted = extract_title_from_line(line, lang, current_krithi['id'])
             if extracted:
                 current_krithi['title'] = extracted
                 current_krithi['title_is_numbered'] = True
                 recent_titles.clear()  # <-- FIX: prevent leaking to the next krithi
                 continue
            
        # Check if line is a section header
        found_sec = None
        for sec_name, sec_pattern in section_patterns.items():
            if sec_pattern.search(line):
                found_sec = sec_name
                break
                
        if found_sec:
            if current_section:
                current_krithi['sections'][current_section] = "\n".join(buffer).strip()
            current_section = found_sec
            buffer = []
            continue
            
        if current_section:
             buffer.append(line)
        else:
             # If we haven't hit a section yet, keep buffering (in case the title comes immediately before pallavi)
             buffer.append(line)

    if current_krithi:
        if current_section:
            current_krithi['sections'][current_section] = "\n".join(buffer).strip()
        krithis.append(current_krithi)

    return krithis

def main():
    base_dir = Path(__file__).resolve().parent
    skt_md_path = base_dir / "mdskt.md"
    eng_md_path = base_dir / "mdeng.md"
    skt_json_path = base_dir / "skt_krithis.json"
    eng_json_path = base_dir / "eng_krithis.json"

    print("Parsing mdskt.md...")
    skt_krithis = extract_krithis_state_machine(str(skt_md_path), lang="skt")
    print(f"Extracted {len(skt_krithis)} Krithis from Sanskrit markdown.")
    
    print("Parsing mdeng.md...")
    eng_krithis = extract_krithis_state_machine(str(eng_md_path), lang="eng")
    print(f"Extracted {len(eng_krithis)} Krithis from English markdown.")
    
    # Validation logic
    print("\n--- Cross-Validation Report ---")
    if len(skt_krithis) != len(eng_krithis):
        print(f"WARNING: Count mismatch! Sanskrit: {len(skt_krithis)} vs English: {len(eng_krithis)}")

    # Post-processing: Title Synchronization
    min_len = min(len(skt_krithis), len(eng_krithis))
    for i in range(min_len):
        skt = skt_krithis[i]
        eng = eng_krithis[i]
        
        # If English title looks wrong (e.g., contains '---', '[^1]', 'talam:'), borrowing the SKT title might be safer
        # or just reporting it
        bad_eng_title = eng['title'] == 'Unknown' or '---' in eng['title'] or '[^' in eng['title'] or 'tālam:' in eng['title'] or len(eng['title'].split()) > 10
        bad_skt_title = skt['title'] == 'Unknown' or '---' in skt['title'] or len(skt['title'].split()) > 10
        
        # Specific fix for titles that start with digits but were caught as "bad" or just need cleaning
        if re.search(r"^[0-9]+\s+(.+)$", eng['title']):
            eng['title'] = re.search(r"^[0-9]+\s+(.+)$", eng['title']).group(1).strip()
            bad_eng_title = False
        
        if bad_eng_title and not bad_skt_title:
             eng['title'] = f"[SYNCED] {skt['title']}"
        elif bad_skt_title and not bad_eng_title:
             skt['title'] = f"[SYNCED] {eng['title']}"

    mismatches = 0
    for i in range(min_len):
        skt = skt_krithis[i]
        eng = eng_krithis[i]
        
        # Check title roughly missing
        if skt['title'] == 'Unknown' or eng['title'] == 'Unknown' or '---' in eng['title'] or '---' in skt['title'] or '[^' in eng['title']:
             print(f"Krithi {i+1} has an obscured title: Skt={skt['title']}, Eng={eng['title']}")
             mismatches += 1

    print(f"\nTotal structural mismatches needing review: {mismatches}")
    
    with open(skt_json_path, "w", encoding="utf-8") as f:
        json.dump(skt_krithis, f, ensure_ascii=False, indent=2)
        
    with open(eng_json_path, "w", encoding="utf-8") as f:
        json.dump(eng_krithis, f, ensure_ascii=False, indent=2)

    print("\nSample Sanskrit Krithi:")
    if skt_krithis:
        print(skt_krithis[0])
        
    print("\nSample English Krithi:")
    if eng_krithis:
        print(eng_krithis[0])

if __name__ == "__main__":
    main()
