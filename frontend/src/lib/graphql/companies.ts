// MOCK — the `companies` GraphQL query doesn't exist yet (see backend/docs/api-contract.md:
// it lands in slice "phase-2/graphql-setup-and-company-queries"). The `companies` table itself
// has no price/marketCap/peRatio columns, so those fields are invented for now and may move to
// a separate query (Redis-backed live quotes) once the real contract lands.
//
// Symbols/sectors/names below match V6__seed_companies.sql so the eventual swap is visually
// seamless. Query args (pagination style, filter param names) are NOT guessed — filtering below
// happens client-side against the full mock list until the real contract defines them.

import type { Company } from '@/types/company'

const MOCK_COMPANIES: Company[] = [
  mockCompany(
    'AAPL',
    'Apple Inc.',
    'Technology',
    'Consumer Electronics',
    'apple.com',
    227.52,
    1.34,
    3.47e12,
    34.2,
  ),
  mockCompany(
    'MSFT',
    'Microsoft Corporation',
    'Technology',
    'Software - Infrastructure',
    'microsoft.com',
    421.9,
    -0.42,
    3.13e12,
    36.1,
  ),
  mockCompany(
    'GOOGL',
    'Alphabet Inc.',
    'Technology',
    'Internet Content & Information',
    'abc.xyz',
    171.34,
    2.11,
    2.11e12,
    25.8,
  ),
  mockCompany(
    'AMZN',
    'Amazon.com, Inc.',
    'Consumer Cyclical',
    'Internet Retail',
    'amazon.com',
    186.21,
    0.87,
    1.95e12,
    41.5,
  ),
  mockCompany(
    'NVDA',
    'NVIDIA Corporation',
    'Technology',
    'Semiconductors',
    'nvidia.com',
    135.58,
    3.92,
    3.32e12,
    55.3,
  ),
  mockCompany(
    'META',
    'Meta Platforms, Inc.',
    'Technology',
    'Internet Content & Information',
    'meta.com',
    563.27,
    -1.15,
    1.43e12,
    27.4,
  ),
  mockCompany(
    'TSLA',
    'Tesla, Inc.',
    'Consumer Cyclical',
    'Auto Manufacturers',
    'tesla.com',
    248.98,
    -3.28,
    793e9,
    null,
  ),
  mockCompany(
    'JPM',
    'JPMorgan Chase & Co.',
    'Financial Services',
    'Banks - Diversified',
    'jpmorganchase.com',
    218.45,
    0.56,
    622e9,
    12.1,
  ),
  mockCompany(
    'V',
    'Visa Inc.',
    'Financial Services',
    'Credit Services',
    'visa.com',
    310.77,
    0.21,
    619e9,
    30.7,
  ),
  mockCompany(
    'JNJ',
    'Johnson & Johnson',
    'Healthcare',
    'Drug Manufacturers - General',
    'jnj.com',
    154.32,
    -0.18,
    371e9,
    15.9,
  ),
  mockCompany(
    'UNH',
    'UnitedHealth Group Incorporated',
    'Healthcare',
    'Healthcare Plans',
    'unitedhealthgroup.com',
    512.6,
    1.02,
    468e9,
    18.3,
  ),
  mockCompany(
    'XOM',
    'Exxon Mobil Corporation',
    'Energy',
    'Oil & Gas Integrated',
    'exxonmobil.com',
    118.9,
    0.64,
    468e9,
    13.6,
  ),
  mockCompany(
    'WMT',
    'Walmart Inc.',
    'Consumer Defensive',
    'Discount Stores',
    'walmart.com',
    92.14,
    0.33,
    741e9,
    38.2,
  ),
  mockCompany(
    'PG',
    'Procter & Gamble Company',
    'Consumer Defensive',
    'Household & Personal Products',
    'pg.com',
    167.88,
    -0.09,
    396e9,
    24.6,
  ),
  mockCompany(
    'DIS',
    'The Walt Disney Company',
    'Communication Services',
    'Entertainment',
    'disney.com',
    112.33,
    1.87,
    205e9,
    38.9,
  ),
  mockCompany(
    'NFLX',
    'Netflix, Inc.',
    'Communication Services',
    'Entertainment',
    'netflix.com',
    897.15,
    2.44,
    386e9,
    47.2,
  ),
  mockCompany(
    'KO',
    'The Coca-Cola Company',
    'Consumer Defensive',
    'Beverages - Non-Alcoholic',
    'coca-colacompany.com',
    71.42,
    0.12,
    308e9,
    27.1,
  ),
  mockCompany(
    'BAC',
    'Bank of America Corporation',
    'Financial Services',
    'Banks - Diversified',
    'bankofamerica.com',
    46.85,
    -0.71,
    361e9,
    14.4,
  ),
  mockCompany(
    'PFE',
    'Pfizer Inc.',
    'Healthcare',
    'Drug Manufacturers - General',
    'pfizer.com',
    26.11,
    -1.44,
    148e9,
    null,
  ),
  mockCompany(
    'AMD',
    'Advanced Micro Devices, Inc.',
    'Technology',
    'Semiconductors',
    'amd.com',
    168.4,
    4.63,
    273e9,
    112.7,
  ),
]

function mockCompany(
  symbol: string,
  name: string,
  sector: string,
  industry: string,
  logoDomain: string,
  price: number,
  changePercent: number,
  marketCap: number,
  peRatio: number | null,
): Company {
  return {
    id: symbol,
    symbol,
    name,
    sector,
    industry,
    description: null,
    logoUrl: `https://logo.clearbit.com/${logoDomain}`,
    exchange: 'NASDAQ',
    price,
    changePercent,
    marketCap,
    peRatio,
  }
}

export interface CompanyFilters {
  search?: string
  sector?: string
}

export async function fetchCompanies(filters: CompanyFilters): Promise<Company[]> {
  await new Promise((resolve) => setTimeout(resolve, 250))

  const search = filters.search?.trim().toLowerCase()
  return MOCK_COMPANIES.filter((company) => {
    const matchesSector =
      !filters.sector || filters.sector === 'All' || company.sector === filters.sector
    const matchesSearch =
      !search ||
      company.symbol.toLowerCase().includes(search) ||
      company.name.toLowerCase().includes(search)
    return matchesSector && matchesSearch
  })
}

export function listSectors(): string[] {
  return Array.from(new Set(MOCK_COMPANIES.map((company) => company.sector))).sort()
}
