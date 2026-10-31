from pathlib import Path
import argparse
import json,shutil
parser = argparse.ArgumentParser(description='Sync shared iOS JSON into Android while retaining platform copy.')
parser.add_argument('--source', type=Path, required=True, help='iOS Resources directory')
source = parser.parse_args().source
target=Path(__file__).resolve().parents[1] / 'app/src/main/res/raw'
for p in source.glob('*.json'):
 if p.name!='ui_translations.json': shutil.copyfile(p,target/p.name)
 else:
  shared=json.loads(p.read_text()); old=json.loads((target/p.name).read_text())
  for key,entry in old.items():
   if key not in shared or key in ['moreinfoview.014','photo.onlineProcessingNotice','photo.onlineFallback']: shared[key]=entry
  (target/p.name).write_text(json.dumps(shared,ensure_ascii=False,indent=2)+'\n')
print('Synced seven shared datasets; retained Android-only, privacy copy.')
