/**
 * Test Address Parsing - Backend to Frontend
 * Sistema de Admisión MTN - October 2025
 *
 * This script tests the address parsing functionality
 * to ensure data flows correctly between backend and frontend
 */

// Simulate the parseAddress function from ApplicationForm.tsx
function parseAddress(fullAddress) {
    if (!fullAddress) {
        return { street: '', number: '', commune: '', apartment: '' };
    }

    // Format expected: "CALLE NUMERO, COMUNA, DEPTO"
    // Example: "AV. PROVIDENCIA 1234, PROVIDENCIA, DEPTO 302"
    const parts = fullAddress.split(',').map(p => p.trim());

    // First part: street and number
    const streetAndNumber = parts[0] || '';
    const lastSpace = streetAndNumber.lastIndexOf(' ');

    const street = lastSpace > 0 ? streetAndNumber.substring(0, lastSpace).trim() : streetAndNumber;
    const number = lastSpace > 0 ? streetAndNumber.substring(lastSpace + 1).trim() : '';

    // Second part: commune
    const commune = parts[1] || '';

    // Third part: apartment (optional)
    const apartment = parts[2] || '';

    return { street, number, commune, apartment };
}

// Simulate combining address (from frontend form)
function combineAddress(street, number, commune, apartment) {
    return `${street} ${number}, ${commune}, ${apartment}`.trim();
}

const colors = {
    green: '\x1b[32m',
    red: '\x1b[31m',
    yellow: '\x1b[33m',
    blue: '\x1b[34m',
    reset: '\x1b[0m'
};

console.log('\n' + '='.repeat(80));
console.log('🧪 TEST: Address Parsing - Backend ↔ Frontend');
console.log('Sistema de Admisión MTN - October 2025');
console.log('='.repeat(80) + '\n');

// Test cases with real data formats
const testCases = [
    {
        name: 'Dirección completa con departamento',
        input: 'AV. PROVIDENCIA 1234, PROVIDENCIA, DEPTO 302',
        expected: {
            street: 'AV. PROVIDENCIA',
            number: '1234',
            commune: 'PROVIDENCIA',
            apartment: 'DEPTO 302'
        }
    },
    {
        name: 'Dirección sin departamento',
        input: 'CALLE LOS OLIVOS 567, SANTIAGO',
        expected: {
            street: 'CALLE LOS OLIVOS',
            number: '567',
            commune: 'SANTIAGO',
            apartment: ''
        }
    },
    {
        name: 'Dirección con casa',
        input: 'PASAJE SAN JUAN 89, LAS CONDES, CASA 15',
        expected: {
            street: 'PASAJE SAN JUAN',
            number: '89',
            commune: 'LAS CONDES',
            apartment: 'CASA 15'
        }
    },
    {
        name: 'Dirección simple',
        input: 'MANUEL MONTT 2500, PROVIDENCIA',
        expected: {
            street: 'MANUEL MONTT',
            number: '2500',
            commune: 'PROVIDENCIA',
            apartment: ''
        }
    },
    {
        name: 'Dirección con avenida larga',
        input: 'AVENIDA LIBERTADOR BERNARDO O\'HIGGINS 3000, SANTIAGO CENTRO',
        expected: {
            street: 'AVENIDA LIBERTADOR BERNARDO O\'HIGGINS',
            number: '3000',
            commune: 'SANTIAGO CENTRO',
            apartment: ''
        }
    }
];

let passed = 0;
let failed = 0;

console.log('📋 TEST 1: Backend → Frontend (Parsing)\n');

testCases.forEach((testCase, index) => {
    console.log(`Test ${index + 1}: ${testCase.name}`);
    console.log(`  Input: "${testCase.input}"`);

    const result = parseAddress(testCase.input);

    const isCorrect =
        result.street === testCase.expected.street &&
        result.number === testCase.expected.number &&
        result.commune === testCase.expected.commune &&
        result.apartment === testCase.expected.apartment;

    if (isCorrect) {
        console.log(`  ${colors.green}✅ PASSED${colors.reset}`);
        console.log(`    Street: "${result.street}"`);
        console.log(`    Number: "${result.number}"`);
        console.log(`    Commune: "${result.commune}"`);
        console.log(`    Apartment: "${result.apartment}"`);
        passed++;
    } else {
        console.log(`  ${colors.red}❌ FAILED${colors.reset}`);
        console.log(`    Expected:`);
        console.log(`      Street: "${testCase.expected.street}"`);
        console.log(`      Number: "${testCase.expected.number}"`);
        console.log(`      Commune: "${testCase.expected.commune}"`);
        console.log(`      Apartment: "${testCase.expected.apartment}"`);
        console.log(`    Got:`);
        console.log(`      Street: "${result.street}"`);
        console.log(`      Number: "${result.number}"`);
        console.log(`      Commune: "${result.commune}"`);
        console.log(`      Apartment: "${result.apartment}"`);
        failed++;
    }
    console.log('');
});

console.log('\n📋 TEST 2: Frontend → Backend (Combining)\n');

// Test round-trip: parse then combine should give same result
testCases.forEach((testCase, index) => {
    console.log(`Test ${index + 1}: Round-trip for "${testCase.name}"`);

    const parsed = parseAddress(testCase.input);
    const combined = combineAddress(parsed.street, parsed.number, parsed.commune, parsed.apartment);

    // Remove trailing commas and spaces for comparison
    const normalizedInput = testCase.input.replace(/,\s*$/, '').trim();
    const normalizedCombined = combined.replace(/,\s*$/, '').trim();

    const isCorrect = normalizedInput === normalizedCombined;

    if (isCorrect) {
        console.log(`  ${colors.green}✅ PASSED${colors.reset}`);
        console.log(`    Original: "${normalizedInput}"`);
        console.log(`    Combined: "${normalizedCombined}"`);
        passed++;
    } else {
        console.log(`  ${colors.red}❌ FAILED${colors.reset}`);
        console.log(`    Original: "${normalizedInput}"`);
        console.log(`    Combined: "${normalizedCombined}"`);
        failed++;
    }
    console.log('');
});

console.log('='.repeat(80));
console.log('📊 RESUMEN DE RESULTADOS');
console.log('='.repeat(80));
console.log(`${colors.green}✅ Pruebas pasadas: ${passed}${colors.reset}`);
console.log(`${colors.red}❌ Pruebas fallidas: ${failed}${colors.reset}`);
console.log(`${colors.blue}ℹ️ Total pruebas: ${passed + failed}${colors.reset}`);

const successRate = ((passed / (passed + failed)) * 100).toFixed(1);
console.log(`\n📈 Tasa de éxito: ${successRate}%`);

if (failed === 0) {
    console.log(`\n${colors.green}🎉 ¡TODOS LOS TESTS PASARON EXITOSAMENTE!${colors.reset}`);
    console.log(`\n${colors.blue}✨ El parseo de direcciones funciona correctamente en ambas direcciones:${colors.reset}`);
    console.log(`   Backend (string combinado) → Frontend (4 campos separados)`);
    console.log(`   Frontend (4 campos separados) → Backend (string combinado)`);
} else {
    console.log(`\n${colors.red}⚠️ Hay ${failed} prueba(s) fallida(s) que requieren atención${colors.reset}`);
}

console.log('='.repeat(80) + '\n');

process.exit(failed === 0 ? 0 : 1);
