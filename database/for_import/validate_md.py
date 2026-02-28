import os
import re

def validate_md(filepath):
    print(f"Validating {filepath}...")
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Split by '# ' at the start of a line to get krithis
    krithis = re.split(r'\n# ', '\n' + content)
    valid_count = 0
    errors = []

    # Filter out empty strings
    krithis = [k.strip() for k in krithis if k.strip()]

    for i, krithi in enumerate(krithis):
        # We split by \n# so the first line is the title without # 
        lines = krithi.split('\n')
        title = lines[0].strip()
        
        # Check if it has an empty title
        if not title:
            errors.append(f"Krithi {i+1} missing Title")
            
        raga_match = re.search(r'\*\*Raga:\*\* (.+)', krithi)
        if not raga_match:
            errors.append(f"Krithi {i+1} ({title[:20]}) missing Raga")
        
        tala_match = re.search(r'\*\*Tala:\*\* (.+)', krithi)
        if not tala_match:
            errors.append(f"Krithi {i+1} ({title[:20]}) missing Tala")
            
        if '## Pallavi' not in krithi and '## Anupallavi' not in krithi and '## Charanam' not in krithi:
             errors.append(f"Krithi {i+1} ({title[:20]}) missing key sections")

        if not any(f"Krithi {i+1}" in e for e in errors):
            valid_count += 1

    print(f"Total krithis found: {len(krithis)}")
    print(f"Valid krithis: {valid_count}")
    if errors:
        print(f"Errors found ({len(errors)}):")
        for e in errors[:10]:
            print(f" - {e}")
            
if __name__ == '__main__':
    base_dir = os.path.dirname(os.path.abspath(__file__))
    validate_md(os.path.join(base_dir, 'final_mdeng.md'))
    validate_md(os.path.join(base_dir, 'final_mdskt.md'))
