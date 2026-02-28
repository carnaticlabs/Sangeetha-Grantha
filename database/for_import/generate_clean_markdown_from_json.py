import json
import csv
import os

def generate_markdown(krithis, output_file):
    order = ['pallavi', 'anupallavi', 'charanam', 'madhyamakala']
    with open(output_file, 'w', encoding='utf-8') as f:
        for krithi in krithis:
            f.write(f"# {krithi.get('title', '')}\n\n")
            f.write(f"**Raga:** {krithi.get('raga', '')}\n")
            f.write(f"**Tala:** {krithi.get('tala', '')}\n\n")
            
            sections = krithi.get('sections', {})
            
            # Write in specific order first
            for key in order:
                if key in sections and sections[key].strip():
                    f.write(f"## {key.capitalize()}\n")
                    f.write(f"{sections[key].strip()}\n\n")
            
            # Write any other keys that were not in the order
            for key, val in sections.items():
                if key not in order and val.strip():
                    f.write(f"## {key.capitalize()}\n")
                    f.write(f"{val.strip()}\n\n")

            f.write("---\n\n")

def main():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    eng_json = os.path.join(base_dir, 'eng_krithis.json')
    skt_json = os.path.join(base_dir, 'skt_krithis.json')

    try:
        with open(eng_json, 'r', encoding='utf-8') as f:
            eng_data = json.load(f)
    except FileNotFoundError:
        eng_data = []

    try:
        with open(skt_json, 'r', encoding='utf-8') as f:
            skt_data = json.load(f)
    except FileNotFoundError:
        skt_data = []

    generate_markdown(eng_data, os.path.join(base_dir, 'final_mdeng.md'))
    generate_markdown(skt_data, os.path.join(base_dir, 'final_mdskt.md'))

    # Create mappings by ID
    eng_dict = {k['id']: k for k in eng_data}
    skt_dict = {k['id']: k for k in skt_data}
    
    all_ids = sorted(list(set(eng_dict.keys()).union(skt_dict.keys())))

    csv_output_file = os.path.join(base_dir, 'krithi_comparison_report.csv')
    with open(csv_output_file, 'w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['Index', 'Title-EN', 'Raga-EN', 'Tala-EN', 'Title-SA', 'Raga-SA', 'Tala-SA'])
        
        for idx in all_ids:
            eng = eng_dict.get(idx, {})
            skt = skt_dict.get(idx, {})
            
            writer.writerow([
                idx,
                eng.get('title', ''), eng.get('raga', ''), eng.get('tala', ''),
                skt.get('title', ''), skt.get('raga', ''), skt.get('tala', '')
            ])

if __name__ == '__main__':
    main()
