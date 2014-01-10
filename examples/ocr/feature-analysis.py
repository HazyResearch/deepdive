import os

targetdir = '../../target/'
weights_tab = [l.rstrip('\n').split('\t') for l in open(targetdir + 'weights.txt').readlines()]
w_fid = {}
for line in weights_tab: 
	f = line[3]
	label = '1(C)'
	if f.startswith('label2'): label = '2(T)' 
	fid = f.rstrip(')').split('Some(')[1]
	w_fid[line[0]] = (label, fid)

weights_val = [l.rstrip('\n').split('\t') for l in open(targetdir + 'inference_result.out.weights').readlines()]

fname = [l.rstrip('\n') for l in open('data/raw/raw-words/feature_names.txt').readlines()]

arr = []

for line in weights_val: 
	wid = line[0]
	wval = float(line[1])
	pair = w_fid[wid]
	arr.append([wval, pair[0], fname[int(pair[1])], pair[1]])

arr.sort(reverse=True)
if not os.path.exists('output'):
	os.makedirs('output')
fout = open('output/feature-analysis.txt', 'w')
print >>fout, '\t'.join(['# Weight', 'Label', 'FeatureName', 'FeatureID'])
for i in range(0, len(arr)):
	arr[i][0] = '%.3f' % arr[i][0]

for line in arr:
	print >>fout, '\t'.join([str(i) for i in line])
fout.close()
