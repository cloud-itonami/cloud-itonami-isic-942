(ns tradeunionorg.render-html
  "Build-time HTML renderer for `docs/samples/operator-console.html`.

  Closes flagship checklist item 2 for `cloud-itonami-isic-942`: this repo
  previously had NO demo page and no generator at all.

  This namespace drives THIS repo's REAL actor stack --
  `tradeunionorg.operation/flow` -> `tradeunionorg.advisor/enrich-proposal`
  -> `tradeunionorg.governor/govern` -> `tradeunionorg.store` -- over a
  single shared `store/demo-store`, and renders the result. Every id,
  member name, event, account, violation string, advisor reasoning and
  confidence score on the page is real output of that run against the real
  seed data in `tradeunionorg.store/demo-store`. Nothing is hand-typed and
  nothing is invented.

  Why NOT `langgraph.graph/run*` (which the isic-9522 reference uses):
  this repo has no langgraph dependency and `tradeunionorg.operation`
  exposes no StateGraph / actor `build` -- its real flow is the plain
  function chain above. Wiring a graph runtime in would be adding a layer
  this repo does not have, i.e. rendering a stack that isn't the one this
  repo ships. We drive the stack that actually exists.

  Determinism: no wall-clock value reaches the page. The store's ledger
  entries DO carry a `java.util.Date` `:timestamp` (see
  `tradeunionorg.operation/commit`); it is deliberately excluded from
  rendering and that exclusion is disclosed on the page itself. All map
  iteration is sorted. Two consecutive runs are byte-identical.

  Build-time invariant: `-main` REFUSES to write the page if the run
  produced zero `:governor-hold` facts. A console that cannot show the
  governor refusing anything is not evidence that the governor works, so
  the HARD-hold requirement is enforced by the build rather than by
  convention.

  Usage: `clojure -M:render-html [out-file]`
  (default `docs/samples/operator-console.html`)."
  (:require [kotoba.lang.text :as str]
            [tradeunionorg.store :as store]
            [tradeunionorg.advisor :as advisor]
            [tradeunionorg.governor :as governor]
            [tradeunionorg.operation :as operation]))

;; ----------------------------- scenario -----------------------------

(def ^:private scenarios
  "The scenario driven through the real stack, in order.

  `:label` is narration; every other cell on the page comes from running
  `:proposal` through `tradeunionorg.operation/flow`. EVERY id on the page
  is one actually seeded by `tradeunionorg.store/demo-store` -- members
  M001/M002/M003, events E001/E002, accounts A001/A002/A003. No id is
  invented, including the ones used to make a check fail.

  The store-lookup arm of HARD check #1 (\"Member not found in store\") is
  fired WITHOUT inventing a missing id: the proposal puts the real account
  id `A001` in the `:member-id` slot. `A001` exists in the store's accounts
  map (it is M001's staff account) but is not a key in its members map, so
  `store/member` returns nil. That is also the realistic operator error --
  pasting an account id into a member field -- rather than a fictional
  member number that could never appear in a real submission.

  Covers: a five-step happy-path lifecycle that commits, six HARD governor
  holds spanning all three checks (including one proposal that trips all
  three at once), two escalations, and one intake rejection that never
  reaches the governor at all."
  [;; ---- happy path: full lifecycle for verified members ----
   {:label "Schedule the monthly general assembly (E001)"
    :group :lifecycle
    :proposal {:operation :schedule-member-meeting
               :effect :propose
               :member-id "M001"
               :event-id "E001"
               :reason "Book Union Hall for the monthly general assembly"}}
   {:label "Coordinate dues-processing logistics"
    :group :lifecycle
    :proposal {:operation :coordinate-dues-processing-logistics
               :effect :propose
               :member-id "M001"
               :reason "Reconcile the monthly dues batch with the finance office"}}
   {:label "Request office consumables for the hall"
    :group :lifecycle
    :proposal {:operation :coordinate-supply-request
               :effect :propose
               :member-id "M001"
               :items "Whiteboard markers, name badges, printer paper"}}
   {:label "Propose a volunteer shift for the training session"
    :group :lifecycle
    :proposal {:operation :schedule-staff-shift-proposal
               :effect :propose
               :member-id "M002"
               :account-id "A002"
               :reason "Offer a voluntary front-desk shift for E002"}}
   {:label "Schedule the new-member training session (E002)"
    :group :lifecycle
    :proposal {:operation :schedule-member-meeting
               :effect :propose
               :member-id "M002"
               :event-id "E002"
               :reason "Book the Training Room for new-member onboarding"}}

   ;; ---- HARD governor holds ----
   {:label "Same booking, but for an unregistered member"
    :group :hold
    :proposal {:operation :schedule-member-meeting
               :effect :propose
               :member-id "M003"
               :event-id "E002"
               :reason "Book the Training Room for new-member onboarding"}}
   {:label "Booking whose member field was filled with an account id (A001)"
    :group :hold
    :proposal {:operation :schedule-member-meeting
               :effect :propose
               :member-id "A001"
               :event-id "E001"
               :reason "Book Union Hall for the monthly general assembly"}}
   {:label "Proposal that tries to act rather than propose"
    :group :hold
    :proposal {:operation :schedule-member-meeting
               :effect :approve
               :member-id "M001"
               :event-id "E001"
               :reason "Book Union Hall for the monthly general assembly"}}
   {:label "Meeting agenda that reaches into collective bargaining"
    :group :hold
    :proposal {:operation :schedule-member-meeting
               :effect :propose
               :member-id "M001"
               :event-id "E001"
               :reason "Agenda: fix our collective bargaining position before the next round"}}
   {:label "Meeting agenda that reaches into union-officer decisions (JA)"
    :group :hold
    :proposal {:operation :schedule-member-meeting
               :effect :propose
               :member-id "M001"
               :event-id "E001"
               :reason "議題: 執行部の役員選出スケジュールを決める"}}
   {:label "Grievance adjudication, unverified member, acting effect"
    :group :hold
    :proposal {:operation :schedule-member-meeting
               :effect :commit
               :member-id "M003"
               :reason "Adjudicate the grievance filed against the shop steward"}}

   ;; ---- escalations ----
   {:label "Member reports an unsafe facility"
    :group :escalate
    :proposal {:operation :flag-safety-concern
               :effect :propose
               :member-id "M001"
               :concern "Blocked fire exit reported in the Union Hall basement"}}
   {:label "Same safety report, filed by an unregistered member"
    :group :escalate
    :proposal {:operation :flag-safety-concern
               :effect :propose
               :member-id "M003"
               :concern "Blocked fire exit reported in the Union Hall basement"}}

   ;; ---- rejected before the governor ever runs ----
   {:label "Supply request submitted with no :effect field"
    :group :intake
    :proposal {:operation :coordinate-supply-request
               :member-id "M001"
               :items "Printer paper"}}])

(defn- fact-of
  "Normalises a real `operation/flow` result into the audit fact keyword
  this console reports. Derived from the run, never asserted."
  [result]
  (cond
    (= :ERROR (:final-status result)) :intake-rejected
    (= :APPROVE (:status result))     :committed
    (= :HOLD (:status result))        :governor-hold
    (= :ESCALATE (:status result))    :escalated
    :else                             :unknown))

(defn run-demo!
  "Runs every scenario through the real stack against ONE shared store, so
  the store's own append-only ledger accumulates exactly what the actor
  actually committed.

  For each scenario we additionally recompute the governor verdict
  independently, by calling `governor/govern` on the same advised proposal
  that `operation/flow` governs. This is not a second opinion -- it is the
  same function on the same input -- but it lets the page show violations
  that `operation/decide` computes and then drops (see the safety-concern
  short-circuit disclosed on the page).

  Returns `{:store store :runs [...]}`."
  []
  (let [db (store/demo-store)
        runs (mapv
              (fn [{:keys [label group proposal]}]
                (let [result (operation/flow db advisor/enrich-proposal
                                             governor/govern proposal)
                      ;; same advisor + governor call `flow` makes, so the
                      ;; verdict shown is the verdict that was computed
                      advised (advisor/enrich-proposal proposal)
                      verdict (governor/govern db advised)]
                  {:label label
                   :group group
                   :proposal proposal
                   :result result
                   :fact (fact-of result)
                   :advisor-reasoning (:advisor-reasoning advised)
                   :confidence (:confidence advised)
                   ;; violations the GOVERNOR found
                   :governor-violations (vec (:violations verdict))
                   ;; violations the DECISION carried forward
                   :reported-violations (vec (:violations result))}))
              scenarios)]
    {:store db :runs runs}))

;; --------------------- measured store behaviour ----------------------

(def ^:private approver-key-candidates
  "Keys a store might plausibly use to record WHO approved a committed
  operation. Scanned against the real committed records at render time so
  that this page self-corrects if the store ever starts recording one."
  [:approver :approver-id :approved-by :approval :actor :actor-id
   :by :operator :operator-id :decided-by :committed-by :reviewer
   :payload :value :signer :authorised-by :authorized-by])

(defn- attribution-audit
  "MEASURES, rather than assumes, whether 'who approved this' can be
  answered from this repo's store. Scans the real committed ledger records
  and the real decision results for any approver-ish key."
  [ledger runs]
  (let [record-keys   (into (sorted-set) (mapcat keys ledger))
        result-keys   (into (sorted-set) (mapcat (comp keys :result) runs))
        in-records    (filterv record-keys approver-key-candidates)
        in-results    (filterv result-keys approver-key-candidates)
        approvals     (filterv #(= :committed (:fact %)) runs)]
    {:record-keys record-keys
     :result-keys result-keys
     :approver-in-records in-records
     :approver-in-results in-results
     :commit-count (count approvals)
     :ledger-count (count ledger)}))

(def ^:private probe-approver-key
  "The approver-identity key the write-path probe supplies. Chosen from
  `approver-key-candidates` so a fix that starts recording it is detected
  by BOTH the probe and the scan of real committed records."
  :approver)

(defn- write-path-probe
  "MEASURES *where* approver attribution is lost, instead of leaving the
  empty approver column ambiguous.

  An absent approver has two very different readings -- 'nobody approved
  it' versus 'someone approved it and the write path silently discarded
  them' -- and the scan above cannot tell them apart, because nothing in
  this stack supplies an approver in the first place. So we supply one and
  watch what happens to it, in two independent probes:

  1. STORE layer: hand `store/append-ledger` a record that already carries
     the key, and see whether the record it stores still has it.
  2. COMMIT layer: put the key on a proposal and run the REAL
     `operation/flow`, then read the record `operation/commit` actually
     wrote.

  Both run against their own throwaway `demo-store`, so the ledger
  rendered on this page is untouched. Every branch of the note is derived
  from these two booleans, so if either layer is ever changed the page
  re-describes itself without anyone editing this namespace."
  []
  (let [;; probe 1 -- the store on its own
        s1        (store/demo-store)
        _         (store/append-ledger s1 {:operation :probe
                                           :status :APPROVE
                                           probe-approver-key "probe-identity"})
        store-rec (first @(:ledger s1))

        ;; probe 2 -- the real flow, approver supplied on the proposal
        s2       (store/demo-store)
        proposal {:operation :schedule-member-meeting
                  :effect :propose
                  :member-id "M001"
                  :event-id "E001"
                  probe-approver-key "probe-identity"
                  :reason "Book Union Hall for the monthly general assembly"}
        result   (operation/flow s2 advisor/enrich-proposal governor/govern proposal)
        flow-rec (first @(:ledger s2))]
    {:store-retains?  (contains? store-rec probe-approver-key)
     :commit-retains? (boolean (and flow-rec (contains? flow-rec probe-approver-key)))
     :flow-status     (:status result)
     :flow-rec-keys   (into (sorted-set) (keys flow-rec))}))

(defn- ledger-coverage
  "MEASURES which dispositions actually reach the store's append-only
  ledger. `operation/commit` is only called on the APPROVE branch, so the
  answer is not 'all of them' -- but we count it rather than assert it."
  [ledger runs]
  (let [by-fact (frequencies (map :fact runs))
        logged  (frequencies (map :status ledger))]
    {:by-fact (into (sorted-map-by compare) (map (fn [[k v]] [(name k) v]) by-fact))
     :logged (into (sorted-map-by compare) (map (fn [[k v]] [(name k) v]) logged))
     :run-count (count runs)
     :ledger-count (count ledger)}))

(defn- dropped-violation-runs
  "MEASURES runs where the real flow DID reach the governor, the governor
  found violations, and the decision did not carry them forward.
  `operation/decide` tests `:flag-safety-concern` BEFORE it tests
  `passes?`, so a safety concern from an unverified member escalates with
  the violation discarded.

  `:intake-rejected` runs are excluded on purpose: for those,
  `operation/flow` returned at `intake` and never called the governor or
  `decide` at all, so nothing was 'dropped by decide'. Counting them here
  would overstate this defect -- they are reported separately by
  `never-governed-runs`."
  [runs]
  (filterv #(and (not= :intake-rejected (:fact %))
                 (seq (:governor-violations %))
                 (empty? (:reported-violations %)))
           runs))

(defn- never-governed-runs
  "MEASURES runs the real flow rejected at `intake`, before the governor
  ran. Any violations shown for these rows come from this renderer's own
  independent `governor/govern` probe, NOT from the flow -- so they are
  labelled as such rather than presented as findings the actor made."
  [runs]
  (filterv #(and (= :intake-rejected (:fact %))
                 (seq (:governor-violations %)))
           runs))

;; ----------------------------- rendering -----------------------------

(defn- esc
  "HTML-escapes a value. Call this on DATA only -- never on a string that
  already contains markup or entities, or the entities get double-escaped
  (`&rarr;` rendering literally as text)."
  [v]
  (-> (str v)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "\"" "&quot;")))

(defn- kw [v] (if (keyword? v) (name v) (str v)))

(defn- code [v] (str "<code>" (esc v) "</code>"))

(defn- fact-badge [fact]
  (let [[cls text] (case fact
                     :committed       ["ok" "committed"]
                     :governor-hold   ["critical" "HARD hold"]
                     :escalated       ["warn" "escalated to human"]
                     :intake-rejected ["muted" "rejected at intake"]
                     ["muted" "unknown"])]
    (str "<span class=\"" cls "\">" text "</span>")))

(defn- violations-cell [violations]
  (if (empty? violations)
    "<span class=\"muted\">none</span>"
    (str/join "<br>"
              (map (fn [v]
                     (str "<code>" (esc (kw (:check/id v))) "</code> &middot; "
                          (esc (:violation v))))
                   violations))))

(defn- member-row [{:keys [id name registered? verified? role]}]
  (format "        <tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>"
          (code id) (esc name) (esc role)
          (if registered? "<span class=\"ok\">registered</span>"
              "<span class=\"critical\">not registered</span>")
          (if verified? "<span class=\"ok\">verified</span>"
              "<span class=\"critical\">not verified</span>")))

(defn- event-row [{:keys [id name date location type]}]
  (format "        <tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>"
          (code id) (esc name) (esc date) (esc location) (esc type)))

(defn- account-row [{:keys [id member-id role status]}]
  (format "        <tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>"
          (code id) (code member-id) (esc role)
          (if (= "active" status) "<span class=\"ok\">active</span>"
              (str "<span class=\"warn\">" (esc status) "</span>"))))

(defn- run-row [{:keys [label proposal result fact confidence
                        governor-violations reported-violations]}]
  (format "        <tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>"
          (esc label)
          (code (kw (:operation proposal)))
          (code (kw (:effect proposal)))
          (if-let [m (:member-id proposal)] (code m) "<span class=\"muted\">&mdash;</span>")
          (str (fact-badge fact)
               (when-let [msg (:message result)]
                 (str "<br><span class=\"muted\">" (esc msg) "</span>")))
          (esc (format "%.2f" (double (or confidence 0.0))))
          (str (violations-cell (if (seq reported-violations)
                                  reported-violations
                                  governor-violations))
               (when (and (seq governor-violations)
                          (empty? reported-violations))
                 (if (= :intake-rejected fact)
                   ;; flow returned at intake -- the governor and decide
                   ;; never ran. Shown only because this renderer probed
                   ;; the governor separately; do not claim decide dropped it.
                   "<br><span class=\"muted\">governor never ran &mdash; rejected at intake; shown from a separate probe</span>"
                   "<br><span class=\"warn\">found by governor, dropped by decide</span>")))))

(defn- ledger-row [{:keys [operation status member-id]}]
  (format "        <tr><td>%s</td><td>%s</td><td>%s</td></tr>"
          (code (kw operation)) (code (kw status))
          (if member-id (code member-id) "<span class=\"muted\">&mdash;</span>")))

(defn- check-row [runs [check-id blurb]]
  (let [fired (count (filter (fn [r]
                               (some #(= check-id (:check/id %))
                                     (:governor-violations r)))
                             runs))]
    (format "        <tr><td>%s</td><td>%s</td><td>%s</td></tr>"
            (code (kw check-id)) (esc blurb)
            (if (pos? fired)
              (str "<span class=\"critical\">fired " fired "&times; in this run</span>")
              "<span class=\"muted\">did not fire in this run</span>"))))

(def ^:private check-descriptions
  ;; The three HARD checks are a fixed contract of `tradeunionorg.governor`
  ;; (see its namespace docstring); the description column documents that
  ;; contract. The "fired" column beside it is counted from the real run.
  [[:member-unverified
    "Member must exist in the store AND be independently :registered? and :verified?. Re-derived from the member record on every proposal, never trusted from the proposal itself."]
   [:effect-not-propose
    "Effect must be :propose. Every operation in the closed allowlist is propose-only; any other effect is refused outright."]
   [:scope-exclusion
    "Permanently blocks collective bargaining, grievance adjudication, strike authorisation, union-leadership/officer decisions and disciplinary action (EN + JA patterns). Only :flag-safety-concern is exempt."]])

(def ^:private page-css
  ;; Deliberately small, self-contained, offline. No jp-go-dds / no vendored
  ;; CSS bundle: this repo's deps are clojure + clojurescript only, and the
  ;; build must stay reproducible without a network fetch. Console quality
  ;; here is HARD-hold count and traceability, not stylesheet bytes.
  (str/join
   "\n"
   [":root{--ink:#1a1a1c;--dim:#5b5b63;--line:#d8d8de;--bg:#f5f5f7;--card:#fff;"
    "--ok:#0b6b3a;--warn:#8a5300;--crit:#a4123f;--ok-bg:#e8f5ee;--warn-bg:#fdf3e2;--crit-bg:#fdecf1;}"
    "*{box-sizing:border-box}"
    "body{margin:0;background:var(--bg);color:var(--ink);"
    "font:15px/1.65 -apple-system,BlinkMacSystemFont,'Hiragino Sans','Noto Sans JP',sans-serif}"
    ".bar{background:#12243d;color:#fff;padding:20px 28px}"
    ".bar h1{margin:0 0 6px;font-size:19px;font-weight:650;letter-spacing:.01em}"
    ".badge{display:inline-block;font-size:12.5px;color:#c3d2e6}"
    "main{max-width:1180px;margin:0 auto;padding:24px 20px 64px}"
    ".card{background:var(--card);border:1px solid var(--line);border-radius:10px;"
    "padding:20px 22px;margin:0 0 20px}"
    ".card h2{margin:0 0 4px;font-size:16px;font-weight:650}"
    ".muted{color:var(--dim)}"
    "p.muted{margin:0 0 14px;font-size:13.5px}"
    "table{width:100%;border-collapse:collapse;font-size:13.5px}"
    "th,td{text-align:left;padding:8px 10px;border-bottom:1px solid var(--line);vertical-align:top}"
    "th{font-weight:640;font-size:12.5px;color:var(--dim);text-transform:uppercase;letter-spacing:.04em}"
    "tbody tr:last-child td{border-bottom:none}"
    "code{font:12.5px/1.5 ui-monospace,SFMono-Regular,Menlo,monospace;"
    "background:#f0f0f4;border-radius:4px;padding:1px 5px}"
    ".ok,.warn,.critical{display:inline-block;border-radius:4px;padding:1px 7px;"
    "font-size:12.5px;font-weight:600;white-space:nowrap}"
    ".ok{color:var(--ok);background:var(--ok-bg)}"
    ".warn{color:var(--warn);background:var(--warn-bg)}"
    ".critical{color:var(--crit);background:var(--crit-bg)}"
    ".note{border-left:3px solid var(--warn);background:var(--warn-bg);"
    "padding:12px 16px;border-radius:0 6px 6px 0;margin:0 0 12px;font-size:13.5px}"
    ".note b{font-weight:650}"
    ".note code{background:#fff}"]))

(defn render
  "Renders the operator console from a completed `run-demo!` result. Every
  value below comes from `runs` or from the real store."
  [{:keys [store runs]}]
  (let [ledger      (vec @(:ledger store))
        members     (sort-by :id (store/all-members store))
        events      (map val (sort-by key (:events store)))
        accounts    (map val (sort-by key (:accounts store)))
        holds       (filterv #(= :governor-hold (:fact %)) runs)
        hold-rules  (into (sorted-set)
                          (map #(kw (:check/id %))
                               (mapcat :reported-violations holds)))
        attribution (attribution-audit ledger runs)
        probe       (write-path-probe)
        coverage    (ledger-coverage ledger runs)
        dropped     (dropped-violation-runs runs)
        ungoverned  (never-governed-runs runs)
        commits     (filterv #(= :committed (:fact %)) runs)
        escalations (filterv #(= :escalated (:fact %)) runs)]
    (str
     "<!doctype html>\n"
     "<html lang=\"en\"><head><meta charset=\"utf-8\">\n"
     "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n"
     "<title>cloud-itonami-isic-942 &middot; trade union administrative coordination</title>\n"
     "<style>\n" page-css "\n</style></head><body>\n"
     "<header class=\"bar\">\n"
     "  <h1>Activities of trade unions (ISIC 942) &mdash; Operator Console</h1>\n"
     "  <span class=\"badge\">read-only build-time sample &middot; governor-gated &middot; every operation is propose-only &middot; bargaining, grievances, strikes, officer decisions and discipline are permanently out of scope</span>\n"
     "</header>\n"
     "<main>\n"

     ;; ---------------- provenance ----------------
     "  <section class=\"card\">\n"
     "    <h2>What this page is</h2>\n"
     "    <p class=\"muted\">Generated at build time by <code>tradeunionorg.render-html</code> (<code>clojure -M:render-html</code>). It drives this repo's real stack &mdash; <code>tradeunionorg.operation/flow</code> &rarr; <code>tradeunionorg.advisor</code> &rarr; <code>tradeunionorg.governor</code> &rarr; <code>tradeunionorg.store</code> &mdash; over one shared <code>store/demo-store</code> and reports what came back. No figure below is hand-written.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Measure</th><th>Value</th></tr></thead>\n"
     "      <tbody>\n"
     (format "        <tr><td>Scenarios driven through the real flow</td><td>%s</td></tr>\n" (count runs))
     (format "        <tr><td>Committed (governor passed)</td><td>%s</td></tr>\n" (count commits))
     (format "        <tr><td>HARD governor holds</td><td><span class=\"critical\">%s</span></td></tr>\n" (count holds))
     (format "        <tr><td>Distinct hold rules exercised</td><td>%s</td></tr>\n"
             (if (seq hold-rules)
               (str/join ", " (map #(str "<code>" (esc %) "</code>") hold-rules))
               "<span class=\"muted\">none</span>"))
     (format "        <tr><td>Escalated to human review</td><td>%s</td></tr>\n" (count escalations))
     (format "        <tr><td>Records in the store's append-only ledger</td><td>%s</td></tr>\n" (count ledger))
     "        <tr><td>Wall-clock values rendered</td><td>0 &mdash; the page is byte-identical across reruns</td></tr>\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"

     ;; ---------------- members ----------------
     "  <section class=\"card\">\n"
     "    <h2>Member directory</h2>\n"
     "    <p class=\"muted\">Read from <code>tradeunionorg.store/demo-store</code>. HARD check #1 re-derives <code>:registered?</code> and <code>:verified?</code> from these records on every single proposal &mdash; a proposal cannot assert its own member's standing.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Member</th><th>Name</th><th>Role</th><th>Registration</th><th>Verification</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" (map member-row members)) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"

     ;; ---------------- events + accounts ----------------
     "  <section class=\"card\">\n"
     "    <h2>Scheduled events</h2>\n"
     "    <p class=\"muted\">Union-local logistics only: meetings, training and social events. Read from the same seeded store.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Event</th><th>Name</th><th>Date</th><th>Location</th><th>Type</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" (map event-row events)) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Staff &amp; volunteer accounts</h2>\n"
     "    <p class=\"muted\">Shift-assignment accounts, each bound to a member id in the directory above.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Account</th><th>Member</th><th>Role</th><th>Status</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" (map account-row accounts)) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"

     ;; ---------------- action gate ----------------
     "  <section class=\"card\">\n"
     "    <h2>Action gate (Union Governor)</h2>\n"
     "    <p class=\"muted\">Three HARD, permanent, un-overridable checks. There is no override path in the code: a violation is a refusal, not a request for approval. The description column documents the fixed contract in <code>tradeunionorg.governor</code>; the right-hand column is counted from this run.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Check</th><th>Contract</th><th>This run</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" (map (partial check-row runs) check-descriptions)) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"

     ;; ---------------- the run ----------------
     "  <section class=\"card\">\n"
     "    <h2>Scenario run (this build)</h2>\n"
     "    <p class=\"muted\">Every row is one call to <code>tradeunionorg.operation/flow</code>: intake &rarr; advise &rarr; govern &rarr; decide &rarr; commit / hold / escalate. Confidence is the advisor's real score for that operation.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Scenario</th><th>Operation</th><th>Effect</th><th>Member</th><th>Disposition</th><th>Conf.</th><th>Governor violations</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" (map run-row runs)) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"

     ;; ---------------- store ledger ----------------
     "  <section class=\"card\">\n"
     "    <h2>Store audit ledger (this run)</h2>\n"
     "    <p class=\"muted\">The append-only ledger inside <code>tradeunionorg.store/MemStore</code>, exactly as <code>tradeunionorg.operation/commit</code> wrote it. Each record also carries a <code>:timestamp</code> (<code>java.util.Date</code>); it is omitted here on purpose so this page stays byte-identical across builds.</p>\n"
     (if (seq ledger)
       (str "    <table>\n"
            "      <thead><tr><th>Operation</th><th>Status</th><th>Member</th></tr></thead>\n"
            "      <tbody>\n"
            (str/join "\n" (map ledger-row ledger)) "\n"
            "      </tbody>\n"
            "    </table>\n")
       "    <p class=\"muted\">No records.</p>\n")
     "  </section>\n"

     ;; ---------------- measured store fidelity ----------------
     "  <section class=\"card\">\n"
     "    <h2>What this store can and cannot tell you</h2>\n"
     "    <p class=\"muted\">Measured at render time by scanning the real records this run produced &mdash; not asserted in the source. If the store is ever changed, these paragraphs change with it.</p>\n"

     ;; approver attribution
     (let [{:keys [record-keys approver-in-records approver-in-results commit-count]} attribution]
       (str
        "    <div class=\"note\">\n"
        "      <b>Approver attribution.</b> "
        (if (seq approver-in-records)
          (format "Committed records carry %s, so &quot;who approved this&quot; is answerable from the store."
                  (str/join ", " (map #(str "<code>" (esc (kw %)) "</code>") approver-in-records)))
          (format (str "Scanned all %s committed record(s); their complete key set is %s. "
                       "None of the %s approver-identity keys this renderer looks for is present, "
                       "and none is present on the decision results either%s. "
                       "This is not the store dropping an approver it was given: "
                       "<code>tradeunionorg.operation/decide</code> returns "
                       "<code>:APPROVE</code> / <code>:HOLD</code> / <code>:ESCALATE</code> and there is "
                       "no human-approval step anywhere in this stack that would produce an identity to record. "
                       "So the honest reading of a committed row below is "
                       "&quot;the governor passed it and the actor committed it&quot;, "
                       "<em>not</em> &quot;a named person approved it&quot;. "
                       "Escalated rows say a human must review, but nothing records who did.")
                  commit-count
                  (str/join ", " (map #(str "<code>" (esc (kw %)) "</code>") record-keys))
                  (count approver-key-candidates)
                  (if (seq approver-in-results)
                    (format " (results carry %s)"
                            (str/join ", " (map #(str "<code>" (esc (kw %)) "</code>") approver-in-results)))
                    "")))
        "\n    </div>\n"))

     ;; where attribution is lost -- probed, not assumed
     (let [{:keys [store-retains? commit-retains? flow-status flow-rec-keys]} probe
           k (str "<code>" (esc (kw probe-approver-key)) "</code>")]
       (str
        "    <div class=\"note\">\n"
        "      <b>Which layer drops the approver.</b> "
        (format (str "&quot;No approver&quot; above could mean nobody approved, or it could mean "
                     "someone did and the write path discarded them. The scan alone cannot tell "
                     "those apart, because nothing in this stack supplies an approver to begin with. "
                     "So this renderer supplies one: it puts %s on a proposal and on a raw ledger "
                     "record, runs both through the real code against throwaway stores, and reports "
                     "what survived. ")
                k)
        (cond
          (and store-retains? (not commit-retains?))
          (format (str "<b>The store is not the culprit.</b> Handed a record that already carried %s, "
                       "<code>MemStore/append-ledger</code> stored it unchanged &mdash; it keeps whatever "
                       "it is given and destructures nothing. But putting %s on a proposal and running "
                       "the real flow (which returned <code>%s</code>) produced a ledger record whose "
                       "complete key set is %s: the approver is gone. "
                       "<code>tradeunionorg.operation/commit</code> does not forward the decision or the "
                       "proposal &mdash; it builds a fresh four-key record from scratch, so every field "
                       "not named in it is dropped at that line. That matters for anyone fixing this: "
                       "adding a human-approval step upstream would <em>not</em> put an approver in the "
                       "ledger on its own, because <code>commit</code> would still discard it.")
                  k k (esc (kw flow-status))
                  (str/join ", " (map #(str "<code>" (esc (kw %)) "</code>") flow-rec-keys)))

          commit-retains?
          (format (str "%s supplied on a proposal now reaches the ledger (record keys: %s), so a "
                       "committed row can name its approver once an approval step actually produces one. "
                       "This paragraph rewrote itself when that became true.")
                  k (str/join ", " (map #(str "<code>" (esc (kw %)) "</code>") flow-rec-keys)))

          (not store-retains?)
          (format (str "<b>The store itself drops it.</b> Handed a record that explicitly carried %s, "
                       "<code>MemStore/append-ledger</code> stored a record without it. Attribution "
                       "cannot be recorded at all until the store stops projecting away keys it is given.")
                  k)

          :else
          (format "Probe inconclusive: store retained %s, flow returned <code>%s</code>."
                  k (esc (kw flow-status))))
        "\n    </div>\n"))

     ;; ledger coverage
     (let [{:keys [by-fact logged run-count ledger-count]} coverage]
       (str
        "    <div class=\"note\">\n"
        "      <b>The ledger only records what succeeded.</b> "
        (format (str "This run produced %s dispositions (%s), but the append-only ledger holds %s record(s) (%s). "
                     "<code>tradeunionorg.operation/flow</code> calls <code>commit</code> only on the "
                     "<code>:APPROVE</code> branch, so every HARD hold and every escalation above leaves "
                     "<em>no trace in the store at all</em>. The refusals on this page exist because the "
                     "renderer captured the return values; an operator reading the ledger alone could not "
                     "reconstruct them. Recording refusals is a governance change with its own contract, "
                     "so it is disclosed here rather than patched by this renderer.")
                run-count
                (str/join ", " (map (fn [[k v]] (format "<code>%s</code> %s" (esc k) v)) by-fact))
                ledger-count
                (if (seq logged)
                  (str/join ", " (map (fn [[k v]] (format "<code>%s</code> %s" (esc k) v)) logged))
                  "empty"))
        "\n    </div>\n"))

     ;; dropped violations
     (if (seq dropped)
       (str
        "    <div class=\"note\">\n"
        "      <b>Some governor findings never reach the decision.</b> "
        (format (str "In %s of the %s scenarios the flow reached the governor, the governor returned "
                     "violations, and the decision did not carry them forward. "
                     "<code>tradeunionorg.operation/decide</code> tests for "
                     "<code>:flag-safety-concern</code> <em>before</em> it tests <code>:passes?</code>, so a "
                     "safety report escalates even when the member who filed it fails HARD check #1. "
                     "Escalating a safety report regardless of the reporter's standing is defensible; "
                     "silently discarding the finding is the part worth knowing &mdash; the escalation "
                     "handed to the human reviewer carries no <code>:violations</code> key at all, so "
                     "nothing tells the reviewer that the reporter failed verification. Affected: %s.")
                (count dropped) (count runs)
                (str/join "; " (map (fn [d]
                                      (format "%s (%s)"
                                              (esc (:label d))
                                              (str/join ", " (map #(str "<code>" (esc (kw (:check/id %))) "</code>")
                                                                  (:governor-violations d)))))
                                    dropped)))
        "\n    </div>\n")
       "")

     ;; rejected before the governor ever ran
     (if (seq ungoverned)
       (str
        "    <div class=\"note\">\n"
        "      <b>Malformed proposals are refused before the governor is consulted.</b> "
        (format (str "%s scenario(s) were rejected by <code>tradeunionorg.operation/intake</code>, which "
                     "returns an <code>:ERROR</code> that makes <code>flow</code> return immediately &mdash; "
                     "the governor and <code>decide</code> never ran for them. The violations shown in "
                     "their row come from a <em>separate</em> <code>governor/govern</code> call this "
                     "renderer makes for comparison, and are labelled in the table as such. They are NOT "
                     "counted as HARD holds anywhere on this page, because the actor never made that "
                     "finding. Affected: %s.")
                (count ungoverned)
                (str/join "; " (map #(esc (:label %)) ungoverned)))
        "\n    </div>\n")
       "")
     "  </section>\n"
     "</main>\n"
     "</body></html>\n")))

(defn -main
  "Regenerates `docs/samples/operator-console.html`.

  REFUSES to write a page from a run that produced no `:governor-hold`
  facts: a console showing only successes is not evidence that the
  governor refuses anything, so the requirement is enforced here at build
  time rather than left to convention."
  [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        {:keys [store runs] :as demo} (run-demo!)
        holds (filterv #(= :governor-hold (:fact %)) runs)
        rules (into (sorted-set) (map #(kw (:check/id %))
                                      (mapcat :reported-violations holds)))]
    (when (empty? holds)
      (throw (ex-info
              (str "Refusing to write " out
                   ": the scenario run produced 0 :governor-hold facts. "
                   "An operator console that never shows the governor refusing anything "
                   "is not evidence that the governor works. Fix the scenario (or the "
                   "governor) so at least one HARD hold is exercised, then re-run.")
              {:out out
               :runs (count runs)
               :facts (frequencies (map :fact runs))})))
    (let [html (render demo)
          f (java.io.File. ^String out)]
      (when-let [p (.getParentFile f)] (.mkdirs p))
      (spit f html)
      (println (format "wrote %s (%d bytes, %d scenarios, %d HARD holds over rules %s, %d ledger records)"
                       out (count (.getBytes ^String html "UTF-8")) (count runs)
                       (count holds) (str/join "/" rules)
                       (count @(:ledger store)))))))
