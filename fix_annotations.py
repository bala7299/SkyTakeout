import os
import re

base_dir = r'd:\Code\SkyTakeout\Backend'
count = 0

for root, dirs, files in os.walk(base_dir):
    for file in files:
        if file.endswith('.java'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            
            new_content = content
            
            # Replace @Operation(value = "...") with @Operation(summary = "...")
            new_content = re.sub(r'@Operation\(value\s*=', '@Operation(summary =', new_content)
            new_content = re.sub(r'@Operation\(value=', '@Operation(summary =', new_content)
            
            # Replace @Tag(tags = "...") with @Tag(name = "...")
            new_content = re.sub(r'@Tag\(tags\s*=', '@Tag(name =', new_content)
            new_content = re.sub(r'@Tag\(tags=', '@Tag(name =', new_content)
            
            if new_content != content:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                count += 1
                print(f'Updated: {filepath}')

print(f'\nTotal files updated: {count}')
