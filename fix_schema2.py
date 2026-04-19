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
            
            # Replace @Schema("xxx") with @Schema(description = "xxx")
            # Match @Schema followed by parentheses with a string literal inside
            new_content = re.sub(r'@Schema\("([^"]*)"\)', r'@Schema(description = "\1")', new_content)
            
            if new_content != content:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                count += 1
                print(f'Updated: {filepath}')

print(f'\nTotal files updated: {count}')
