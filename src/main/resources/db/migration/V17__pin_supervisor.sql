ALTER TABLE usuarios ADD COLUMN pin_hash VARCHAR(64);
ALTER TABLE usuarios ADD COLUMN pin_sal VARCHAR(32);

UPDATE usuarios SET pin_sal = '15bbe9cf6a1eae142bf43a492982333b',
    pin_hash = '3554f3ac272630cd32d5dc1619e904d33d18ddbf8041e48946537674a683e4c2'
WHERE login = 'admin' AND pin_hash IS NULL;

UPDATE usuarios SET pin_sal = '58a17b5a28e7eeaa64ea5cd9ed660222',
    pin_hash = 'd2b8f5a1a7eba7b1c3640ec9f4ea3b84a386807c309bcd11a2bd921ec45cffd2'
WHERE login = 'operador' AND pin_hash IS NULL;
