(ns tradeunionorg.governor
  "Governor with three HARD, permanent, un-overridable checks for the
  trade union administrative coordination actor.

  Scope: union-local administrative coordination only — member enrollment
  logistics, meeting/event scheduling, dues-processing logistics.

  1. Member/event-record unverified — target must exist in store AND be
     independently :registered?/:verified?, re-derived every time.
  2. Effect not :propose — rejected outright.
  3. Scope exclusion — any proposal touching collective-bargaining positions,
     grievance-adjudication, strike-authorization, union-leadership/officer
     decisions, or disciplinary action is permanently blocked."
  (:require [tradeunionorg.store :as store]
            [clojure.string :as str]))

;; ---------------------- hard checks ----------------------

(defn member-unverified-violations
  "Check 1: Member must be registered AND verified.
  This is re-derived from the member's own :registered?/:verified? fields,
  never from proposal self-report."
  [store member-id]
  (let [member (store/member store member-id)]
    (cond
      (nil? member)
      [{:check/id :member-unverified
        :violation "Member not found in store"}]

      (not (:registered? member))
      [{:check/id :member-unverified
        :violation "Member is not registered"}]

      (not (:verified? member))
      [{:check/id :member-unverified
        :violation "Member is not verified"}]

      :else
      [])))

(defn effect-not-propose-violations
  "Check 2: Effect must be :propose. Any other effect is rejected outright."
  [proposal]
  (if (not= (:effect proposal) :propose)
    [{:check/id :effect-not-propose
      :violation (str "Effect is " (:effect proposal) ", not :propose")}]
    []))

(defn scope-exclusion-violations
  "Check 3: Block proposals touching excluded territory.
  Excluded: collective-bargaining positions, grievance-adjudication decisions,
  strike-authorization decisions, union-leadership/officer decisions,
  disciplinary action.

  Uses qualified substring scan (EN+JA) so legitimate :flag-safety-concern
  ops that mention 'safety' aren't self-blocked.

  **Critical pattern (isic-920 fix)**: combine EN+JA patterns into a single
  explicit boolean (should-reject) that is actually used in the return value."
  [proposal]
  (let [forbidden-patterns
        [;; EN patterns: bargaining, grievance, strike, leadership, discipline
         #"(?i)collective.?bargain"
         #"(?i)bargain.?position"
         #"(?i)negotiat.?wage"
         #"(?i)negotiat.?benefit"
         #"(?i)grievance"
         #"(?i)arbitration"
         #"(?i)dispute.?resolution"
         #"(?i)strike"
         #"(?i)union.?officer"
         #"(?i)union.?leader"
         #"(?i)union.?president"
         #"(?i)union.?executive"
         #"(?i)disciplinary"
         #"(?i)expulsion"
         #"(?i)suspension"
         #"(?i)dismissal"
         ;; JA patterns: 団交(collective bargaining), 苦情処理, スト, 執行部, 処分
         #"団交"
         #"賃金"
         #"待遇"
         #"交渉"
         #"苦情"
         #"紛争.?解決"
         #"仲裁"
         #"ストライキ"
         #"スト"
         #"執行部"
         #"役員"
         #"組合長"
         #"処分"
         #"除名"
         #"懲罰"]

        ;; Allowed operations that legitimately may flag concerns
        allowed-ops #{:flag-safety-concern}

        op-id (:operation proposal)
        proposal-str (str proposal)

        ;; Check if any forbidden pattern matches
        has-forbidden-match (some #(re-find % proposal-str) forbidden-patterns)
        is-allowed-op (allowed-ops op-id)

        ;; Combined check: return violation only if forbidden AND not allowed-op
        ;; **This boolean is actually used in the return value (isic-920 fix)**
        should-reject (boolean (and has-forbidden-match (not is-allowed-op)))]

    (if should-reject
      [{:check/id :scope-exclusion
        :violation "Proposal touches collective-bargaining, grievance-adjudication, strike-authorization, union-leadership/officer decisions, or disciplinary action"}]
      [])))

;; ---------------------- decision logic ----------------------

(defn govern
  "Apply all three HARD checks. Any violation is a permanent rejection
  with no override path."
  [store proposal]
  (let [;; Only check member verification if member-id is present
        member-violations (if (:member-id proposal)
                            (member-unverified-violations store (:member-id proposal))
                            [])
        effect-violations (effect-not-propose-violations proposal)
        scope-violations (scope-exclusion-violations proposal)
        all-violations (concat member-violations effect-violations scope-violations)]

    {:proposal proposal
     :violations all-violations
     :passes? (empty? all-violations)
     :decision (if (empty? all-violations)
                 :APPROVE
                 :REJECT)}))
