CREATE INDEX idx_research_notes_user_id ON research_notes(user_id);
CREATE INDEX idx_research_notes_company_id ON research_notes(company_id);
CREATE INDEX idx_research_notes_user_company ON research_notes(user_id, company_id);
