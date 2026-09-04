// Copia el build de producción de Vite (dist/) a los recursos estáticos de Spring Boot.
import { cpSync, rmSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const frontendDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const source = path.join(frontendDir, 'dist');
const target = path.join(frontendDir, '..', 'backend', 'src', 'main', 'resources', 'static');

rmSync(target, { recursive: true, force: true });
cpSync(source, target, { recursive: true });
console.log(`Build copiado a: ${target}`);
