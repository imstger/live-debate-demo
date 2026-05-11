const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const h5Root = path.join(root, 'dist', 'build', 'h5');
const userH5Source = path.join(root, 'dist', 'dev', 'h5');
const adminSource = path.join(root, 'admin');
const adminTarget = path.join(h5Root, 'admin');
const staticSource = path.join(root, 'static');
const staticTarget = path.join(h5Root, 'static');

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

if (!fs.existsSync(adminSource)) {
  console.warn('[copy-admin-to-h5] admin directory not found, skipped.');
  process.exit(0);
}

if (fs.existsSync(userH5Source)) {
  copyDir(userH5Source, h5Root);
  console.log(`[copy-admin-to-h5] copied user h5 app from ${userH5Source} to ${h5Root}`);
} else {
  console.warn('[copy-admin-to-h5] user h5 app not found, only admin assets will be copied.');
}

copyDir(adminSource, adminTarget);
console.log(`[copy-admin-to-h5] copied admin assets to ${adminTarget}`);

if (fs.existsSync(staticSource)) {
  copyDir(staticSource, staticTarget);
  console.log(`[copy-admin-to-h5] copied static assets to ${staticTarget}`);
} else {
  console.warn('[copy-admin-to-h5] static directory not found, skipped.');
}
