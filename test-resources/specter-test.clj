(do
  (use 'com.rpl.specter)
  (def res
    [(transform [MAP-VALS MAP-VALS]
                inc
                {:a {:aa 1} :b {:ba -1 :bb 2}})
     (select [ALL ALL #(= 0 (mod % 3))]
             [[1 2 3 4] [] [5 3 2 18] [2 4 6] [12]])

     (transform [(filterer odd?) LAST]
                inc
                [2 1 3 6 9 4 8])

     (setval [:a ALL nil?] NONE {:a [1 2 nil 3 nil]})

     (setval [:a :b :c] NONE {:a {:b {:c 1}}})

     (setval [:a (compact :b :c)] NONE {:a {:b {:c 1}}})

     (transform [(srange 1 4) ALL odd?] inc [0 1 2 3 4 5 6 7])

     (setval (srange 2 4) [:a :b :c :d :e] [0 1 2 3 4 5 6 7 8 9])

     (setval [ALL END] [:a :b] [[1] '(1 2) [:c]])

     (select (walker number?)
             {2 [1 2 [6 7]] :a 4 :c {:a 1 :d [2 nil]}})

     (select ["a" "b"]
             {"a" {"b" 10}})

     (transform [(srange 4 11) (filterer even?)]
                reverse
                [0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15])

     (setval [ALL
              (selected? (filterer even?) (view count) (pred>= 2))
              END]
             [:c :d]
             [[1 2 3 4 5 6] [7 0 -1] [8 8] []])

     (transform [ALL (collect-one :b) :a even?]
                +
                [{:a 1 :b 3} {:a 2 :b -10} {:a 4 :b 10} {:a 3}])

     (transform [:a (putval 10)]
                +
                {:a 1 :b 3})

     (transform [ALL (if-path [:a even?] [:c ALL] :d)]
                inc
                [{:a 2 :c [1 2] :d 4} {:a 4 :c [0 10 -1]} {:a -1 :c [1 1 1] :d 1}])])

  (constantly res))

