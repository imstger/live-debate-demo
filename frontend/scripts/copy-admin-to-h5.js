const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const source = path.join(root, 'admin');
const target = path.join(root, 'dist', 'build', 'h5', 'admin');

function copyDir(from, to) {
  fs.mkdirSync(to, { recursive: true });
  for (const entry of fs.readdirSync(from, { withFileTypes: true })) {
    const sourcePath = path.join(from, entry.name);
    const targetPath = path.join(to, entry.name);
    if (entry.isDirectory()) {
      copyDir(sourcePath, targetPath);
    } else {
      fs.copyFileSync(sourcePath, targetPath);
    }
  }
}

if (!fs.existsSync(source)) {
  console.warn('[copy-admin-to-h5] admin directory not found, skipped.');
  process.exit(0);
}

copyDir(source, target);
console.log(`[copy-admin-to-h5] copied admin assets to ${target}`);
