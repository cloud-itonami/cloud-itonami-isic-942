(ns tradeunionorg.operation
  "Operation flow: StateGraph-style intake → advise → govern → decide → commit | hold | escalate.

  All operations in the closed allowlist are :propose-only. Escalation paths
  (safety concerns, governance hold) are routed to human review."
  (:require [tradeunionorg.store :as store]))

(defn now-timestamp []
  #?(:cljs (js/Date.)
     :clj  (java.util.Date.)))

(defn intake
  "Validate basic proposal structure."
  [proposal]
  (cond
    (nil? (:operation proposal))
    {:status :ERROR, :message "Missing :operation field"}
    (nil? (:effect proposal))
    {:status :ERROR, :message "Missing :effect field"}
    :else
    {:status :OK, :proposal proposal}))

(defn advise
  "Enrich proposal with advisor reasoning (assumes advisor module available)."
  [advisor-fn proposal]
  (assoc proposal :advised? true
         :advisor-reasoning (:advisor-reasoning (advisor-fn proposal))
         :confidence (:confidence (advisor-fn proposal))))

(defn govern
  "Apply governor checks (assumes governor module available)."
  [governor-fn store proposal]
  (governor-fn store proposal))

(defn decide
  "Decision logic: APPROVE if governance passes, ESCALATE if safety concern,
  HOLD if governance fails, ESCALATE if other rejections."
  [governance-result proposal]
  (let [{:keys [decision passes? violations]} governance-result
        op-id (:operation proposal)
        is-safety-concern (= op-id :flag-safety-concern)]

    (cond
      ;; Safety concerns always escalate, even if governance passes
      is-safety-concern
      {:status :ESCALATE
       :reason "Safety concern requires human review"
       :proposal proposal}

      ;; Governance passed, all other ops commit
      passes?
      {:status :APPROVE
       :reason "Governance passed"
       :proposal proposal}

      ;; Governance failed, hold for escalation
      :else
      {:status :HOLD
       :reason "Governance failed"
       :violations violations
       :proposal proposal})))

(defn commit
  "Execute approved operation (demo: just log to ledger)."
  [store decision]
  (let [proposal (:proposal decision)]
    (store/append-ledger store
                         {:timestamp (now-timestamp)
                          :operation (:operation proposal)
                          :status (:status decision)
                          :member-id (:member-id proposal)})
    decision))

(defn flow
  "End-to-end operation flow: intake → advise → govern → decide → (commit | hold | escalate)."
  [store advisor-fn governor-fn proposal]
  (let [intake-result (intake proposal)]
    (if (= (:status intake-result) :ERROR)
      {:final-status :ERROR, :message (:message intake-result)}

      (let [advised (advise advisor-fn (:proposal intake-result))
            governed (govern governor-fn store advised)
            decided (decide governed advised)
            committed (if (= (:status decided) :APPROVE)
                        (commit store decided)
                        decided)]
        committed))))
