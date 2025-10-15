#!/usr/bin/env node

const BASE_URL = 'http://localhost:8080';

async function testInterviewEndpoint() {
  console.log('🧪 Testing POST /api/interviews endpoint\n');
  
  const tests = [
    {
      name: 'Invalid type (DIRECTIVA) should return 400',
      payload: {
        applicationId: 1,
        interviewerId: 24,
        scheduledDate: '2025-09-25T14:00:00',
        type: 'DIRECTIVA',
        mode: 'IN_PERSON'
      },
      expectedStatus: 400
    },
    {
      name: 'Valid type (INDIVIDUAL) should return 201',
      payload: {
        applicationId: 1,
        interviewerId: 24,
        scheduledDate: '2025-09-26T15:00:00',
        type: 'INDIVIDUAL',
        mode: 'IN_PERSON',
        location: 'Oficina Principal'
      },
      expectedStatus: 201
    },
    {
      name: 'Past date should return 400',
      payload: {
        applicationId: 1,
        interviewerId: 24,
        scheduledDate: '2025-09-20T14:00:00',
        type: 'PSYCHOLOGICAL',
        mode: 'VIRTUAL'
      },
      expectedStatus: 400
    },
    {
      name: 'Missing required fields should return 400',
      payload: {
        applicationId: 1,
        scheduledDate: '2025-09-27T10:00:00',
        type: 'ACADEMIC'
      },
      expectedStatus: 400
    },
    {
      name: 'Valid psychological interview should return 201',
      payload: {
        applicationId: 1,
        interviewerId: 24,
        scheduledDate: '2025-09-28T11:00:00',
        duration: 90,
        type: 'PSYCHOLOGICAL',
        mode: 'IN_PERSON',
        location: 'Oficina Psicología',
        notes: 'Evaluación inicial'
      },
      expectedStatus: 201
    }
  ];

  let passed = 0;
  let failed = 0;

  for (const test of tests) {
    try {
      console.log(`🔍 ${test.name}`);
      
      const response = await fetch(`${BASE_URL}/api/interviews`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(test.payload)
      });

      const data = await response.json();
      
      if (response.status === test.expectedStatus) {
        console.log(`✅ PASS - Status: ${response.status}`);
        if (test.expectedStatus === 201) {
          console.log(`   Created interview ID: ${data.data?.id}`);
        } else {
          console.log(`   Error: ${data.error}`);
        }
        passed++;
      } else {
        console.log(`❌ FAIL - Expected: ${test.expectedStatus}, Got: ${response.status}`);
        console.log(`   Response: ${JSON.stringify(data, null, 2)}`);
        failed++;
      }
      
    } catch (error) {
      console.log(`❌ FAIL - Network error: ${error.message}`);
      failed++;
    }
    
    console.log('');
  }

  console.log('📊 Test Results:');
  console.log(`✅ Passed: ${passed}`);
  console.log(`❌ Failed: ${failed}`);
  console.log(`📈 Success Rate: ${Math.round((passed / (passed + failed)) * 100)}%`);

  process.exit(failed > 0 ? 1 : 0);
}

// Check if running directly
if (require.main === module) {
  testInterviewEndpoint().catch(console.error);
}

module.exports = { testInterviewEndpoint };