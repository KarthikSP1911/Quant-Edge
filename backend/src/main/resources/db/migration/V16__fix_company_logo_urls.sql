-- Clearbit's free public Logo API (logo.clearbit.com) was discontinued, so the URLs seeded in
-- V6__seed_companies.sql no longer resolve. Switch to Google's favicon service, keyed by the
-- same company domains, which has no API key and no rate limit for this volume of traffic.
UPDATE companies SET logo_url = 'https://www.google.com/s2/favicons?domain=apple.com&sz=128' WHERE symbol = 'AAPL';
UPDATE companies SET logo_url = 'https://www.google.com/s2/favicons?domain=microsoft.com&sz=128' WHERE symbol = 'MSFT';
UPDATE companies SET logo_url = 'https://www.google.com/s2/favicons?domain=abc.xyz&sz=128' WHERE symbol = 'GOOGL';
UPDATE companies SET logo_url = 'https://www.google.com/s2/favicons?domain=amazon.com&sz=128' WHERE symbol = 'AMZN';
UPDATE companies SET logo_url = 'https://www.google.com/s2/favicons?domain=nvidia.com&sz=128' WHERE symbol = 'NVDA';
UPDATE companies SET logo_url = 'https://www.google.com/s2/favicons?domain=meta.com&sz=128' WHERE symbol = 'META';
UPDATE companies SET logo_url = 'https://www.google.com/s2/favicons?domain=tesla.com&sz=128' WHERE symbol = 'TSLA';
UPDATE companies SET logo_url = 'https://www.google.com/s2/favicons?domain=jpmorganchase.com&sz=128' WHERE symbol = 'JPM';
UPDATE companies SET logo_url = 'https://www.google.com/s2/favicons?domain=visa.com&sz=128' WHERE symbol = 'V';
UPDATE companies SET logo_url = 'https://www.google.com/s2/favicons?domain=jnj.com&sz=128' WHERE symbol = 'JNJ';
UPDATE companies SET logo_url = 'https://www.google.com/s2/favicons?domain=unitedhealthgroup.com&sz=128' WHERE symbol = 'UNH';
UPDATE companies SET logo_url = 'https://www.google.com/s2/favicons?domain=exxonmobil.com&sz=128' WHERE symbol = 'XOM';
UPDATE companies SET logo_url = 'https://www.google.com/s2/favicons?domain=walmart.com&sz=128' WHERE symbol = 'WMT';
UPDATE companies SET logo_url = 'https://www.google.com/s2/favicons?domain=pg.com&sz=128' WHERE symbol = 'PG';
UPDATE companies SET logo_url = 'https://www.google.com/s2/favicons?domain=disney.com&sz=128' WHERE symbol = 'DIS';
UPDATE companies SET logo_url = 'https://www.google.com/s2/favicons?domain=netflix.com&sz=128' WHERE symbol = 'NFLX';
UPDATE companies SET logo_url = 'https://www.google.com/s2/favicons?domain=coca-colacompany.com&sz=128' WHERE symbol = 'KO';
UPDATE companies SET logo_url = 'https://www.google.com/s2/favicons?domain=bankofamerica.com&sz=128' WHERE symbol = 'BAC';
UPDATE companies SET logo_url = 'https://www.google.com/s2/favicons?domain=pfizer.com&sz=128' WHERE symbol = 'PFE';
UPDATE companies SET logo_url = 'https://www.google.com/s2/favicons?domain=amd.com&sz=128' WHERE symbol = 'AMD';
