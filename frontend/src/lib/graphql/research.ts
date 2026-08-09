export const GET_RESEARCH_NOTES = `
  query GetResearchNotes($symbol: String) {
    researchNotes(symbol: $symbol) {
      id
      company {
        symbol
        name
      }
      title
      content
      generatedBy
      createdAt
    }
  }
`
