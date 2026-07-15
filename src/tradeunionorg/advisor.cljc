(ns tradeunionorg.advisor
  "Advisor: enrich proposals with reasoning and confidence scores.
  Deterministic demo implementation; production requires real LLM with
  prompt injection safeguards.")

(defn enrich-proposal
  "Add :advisor-reasoning and :confidence to a proposal.
  Preserves original proposal fields unchanged.
  Deterministic demo logic based on operation type."
  [proposal]
  (let [op-id (:operation proposal)
        reasoning (case op-id
                    :schedule-member-meeting
                    "Meeting scheduling supports administrative coordination. Member and event verified."
                    :coordinate-dues-processing-logistics
                    "Dues tracking is legitimate administrative logistics. No financial decision involved."
                    :coordinate-supply-request
                    "Supply request for office consumables is routine logistics. No policy content."
                    :schedule-staff-shift-proposal
                    "Staff shift proposal supports voluntary shift coordination. Administrative logistics only."
                    :flag-safety-concern
                    "Safety concern flagged for human escalation. Forwarding to leadership for review."
                    "Operation requires advisor review.")

        confidence (case op-id
                     :schedule-member-meeting 0.92
                     :coordinate-dues-processing-logistics 0.88
                     :coordinate-supply-request 0.85
                     :schedule-staff-shift-proposal 0.80
                     :flag-safety-concern 0.95
                     0.5)]

    (assoc proposal
           :advisor-reasoning reasoning
           :confidence confidence)))
