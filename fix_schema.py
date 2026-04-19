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
            
            # Replace @Schema(value = "...") with @Schema(description = "...")
            new_content = re.sub(r'@Schema\(value\s*=', '@Schema(description =', new_content)
            
            # Replace @Schema(value="...") with @Schema(description="...")
            new_content = re.sub(r'@Schema\(value=', '@Schema(description=', new_content)
            
            if new_content != content:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                count += 1
                print(f'Updated: {filepath}')

print(f'\nTotal files updated: {count}')
