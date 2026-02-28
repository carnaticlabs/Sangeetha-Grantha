import csv
import json

with open('skt_krithis.json', 'r') as f:
    skt = json.load(f)
with open('eng_krithis.json', 'r') as f:
    eng = json.load(f)

# Ensure lengths match before writing
min_len = min(len(skt), len(eng))

with open('krithi_comparison_report.csv', 'w', newline='', encoding='utf-8') as csvfile:
    writer = csv.writer(csvfile)
    writer.writerow(['Index', 'Title-EN', 'Raga-EN', 'Tala-EN', 'Title-SA', 'Raga-SA', 'Tala-SA'])
    
    for i in range(min_len):
        e = eng[i]
        s = skt[i]
        
        # Strip confusing synced prefixes in report for cleaner look
        title_en = e['title'].replace('[SYNCED] ', '')
        title_sa = s['title'].replace('[SYNCED] ', '')
        
        writer.writerow([
            i+1,
            title_en,
            e['raga'],
            e['tala'],
            title_sa,
            s['raga'],
            s['tala']
        ])
        
print(f"Comparison CSV created with {min_len} rows: krithi_comparison_report.csv")
