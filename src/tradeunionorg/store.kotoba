(ns tradeunionorg.store
  "Store protocol and in-memory MemStore implementation for the trade union
  administrative coordination actor.

  Union members (dues-paying, verified), events (meetings, training, social),
  and administrative accounts (staff shift assignments) are stored in a
  directory-style structure with append-only ledger for audit trail.

  Production: replace with persistent backing (EDN file, database, ledger).")

(defprotocol Store
  "Minimal protocol for union member, event, and account access."
  (member [store member-id] "Fetch member by ID")
  (all-members [store] "Fetch all members")
  (event [store event-id] "Fetch event by ID")
  (account [store account-id] "Fetch account (staff assignment) by ID")
  (append-ledger [store entry] "Append entry to audit ledger"))

(defrecord MemStore [members events accounts ledger]
  Store
  (member [_store member-id]
    (get members member-id))
  (all-members [_store]
    (vals members))
  (event [_store event-id]
    (get events event-id))
  (account [_store account-id]
    (get accounts account-id))
  (append-ledger [store entry]
    (swap! ledger conj entry)
    store))

;; ---------------------- demo data ----------------------

(defn demo-store
  "Create a MemStore with demo union members, events, and staff accounts.
  All demo members are registered and verified."
  []
  (let [members
        {"M001" {:id "M001"
                 :name "Alice Tanaka"
                 :registered? true
                 :verified? true
                 :role "Shop Steward"}
         "M002" {:id "M002"
                 :name "Bob Suzuki"
                 :registered? true
                 :verified? true
                 :role "Member"}
         "M003" {:id "M003"
                 :name "Carol Yamamoto"
                 :registered? false  ;; unverified for testing
                 :verified? false
                 :role "Member"}}

        events
        {"E001" {:id "E001"
                 :name "Monthly General Assembly"
                 :date "2026-08-15"
                 :location "Union Hall"
                 :type "meeting"}
         "E002" {:id "E002"
                 :name "New Member Training"
                 :date "2026-08-20"
                 :location "Training Room"
                 :type "training"}}

        accounts
        {"A001" {:id "A001"
                 :member-id "M001"
                 :role "staff"
                 :status "active"}
         "A002" {:id "A002"
                 :member-id "M002"
                 :role "volunteer"
                 :status "active"}
         "A003" {:id "A003"
                 :member-id "M003"
                 :role "volunteer"
                 :status "pending"}}

        ledger (atom [])]

    (MemStore. members events accounts ledger)))
