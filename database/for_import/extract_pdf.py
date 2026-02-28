import fitz

def analyze_pdf(path, start_page=16, num_pages=2):
    print(f'\n--- Analyzing {path} ---')
    doc = fitz.open(path)
    for i in range(start_page, min(start_page + num_pages, len(doc))):
        page = doc[i]
        blocks = page.get_text('blocks')
        print(f'Page {i+1}:')
        for b in blocks:
            text = b[4].strip().replace('\n', ' ')
            if text:
                print(f'  [Block]: {repr(text)}')

analyze_pdf('mdskt.pdf')
analyze_pdf('mdeng.pdf')
