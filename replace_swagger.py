import os

replacements = [
    ('import io.swagger.annotations.Api;', 'import io.swagger.v3.oas.annotations.tags.Tag;'),
    ('import io.swagger.annotations.ApiOperation;', 'import io.swagger.v3.oas.annotations.Operation;'),
    ('import io.swagger.annotations.ApiModel;', 'import io.swagger.v3.oas.annotations.media.Schema;'),
    ('import io.swagger.annotations.ApiModelProperty;', 'import io.swagger.v3.oas.annotations.media.Schema;'),
    ('@Api(', '@Tag('),
    ('@ApiOperation(', '@Operation('),
    ('@ApiModel(', '@Schema('),
    ('@ApiModelProperty(', '@Schema('),
]

base_dir = r'd:\Code\SkyTakeout\Backend'
count = 0

for root, dirs, files in os.walk(base_dir):
    for file in files:
        if file.endswith('.java'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            
            new_content = content
            for old, new in replacements:
                new_content = new_content.replace(old, new)
            
            if new_content != content:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                count += 1
                print(f'Updated: {filepath}')

print(f'\nTotal files updated: {count}')
