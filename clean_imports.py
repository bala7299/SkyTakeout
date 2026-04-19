import os

base_dir = r'd:\Code\SkyTakeout\Backend'
count = 0

for root, dirs, files in os.walk(base_dir):
    for file in files:
        if file.endswith('.java'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                lines = f.readlines()
            
            # 去重 import 语句
            seen_imports = {}
            new_lines = []
            for line in lines:
                if line.strip().startswith('import '):
                    import_stmt = line.strip()
                    if import_stmt not in seen_imports:
                        seen_imports[import_stmt] = True
                        new_lines.append(line)
                else:
                    new_lines.append(line)
            
            new_content = ''.join(new_lines)
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
            count += 1

print(f'Cleaned {count} files')
