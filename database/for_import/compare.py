import json

with open('skt_krithis.json', 'r') as f:
    skt = json.load(f)
with open('eng_krithis.json', 'r') as f:
    eng = json.load(f)

# The Sanskrit alphabet maps roughly to ASCII
# Let's just compare the first 2 chars of the raga after normalizing

def norm(r):
    r = r.lower().replace('ā', 'a').replace('ī','i').replace('ū','u')
    return r.split('(')[0].strip()

# Print out parallel ragas until they mismatch
for i in range(min(len(skt), len(eng))):
    s_raga = skt[i]['raga'].split('(')[0].strip()
    e_raga = eng[i]['raga'].split('(')[0].strip()
    
    # We can just look for the row number 484 divergence
    print(f"{i+1:03d} | SKT: {s_raga[:10]:<15} | ENG: {e_raga[:10]:<15}")
