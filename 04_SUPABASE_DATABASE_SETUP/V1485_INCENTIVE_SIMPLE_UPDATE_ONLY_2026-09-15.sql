-- ⚠️ এটাও V1484-এর মতোই — নতুন কোনো নিয়ম নয়, শুধু একদম ছোট করে একটাই Update
-- statement (আগেরটা লম্বা ছিল বলে হয়তো পুরোটা কপি হয়নি)। এটা Run করার পর
-- উপরে ঠিক কী লেখা আসে (যেমন "Success. X rows affected") সেটা জানাবেন।

update hr.salary_payments s
set amount = w.amount, extra_reason = w.reason
from hr.incentive_wanted() w
where s.src_key = w.src_key and s.status = 'DUE';
