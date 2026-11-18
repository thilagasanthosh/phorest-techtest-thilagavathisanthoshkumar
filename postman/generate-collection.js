// Generates postman/fruitmachine-api.postman_collection.json (Postman v2.1).
// Usage: node postman/generate-collection.js postman/fruitmachine-api.postman_collection.json
const fs = require("fs");
const path = require("path");

const OUT = process.argv[2];

// Shared assertion helpers, prepended to every test script.
const HELPERS = [
  "function problem(status, type) {",
  "  pm.test('status ' + status, function () { pm.response.to.have.status(status); });",
  "  pm.test('RFC 7807 problem type ' + type, function () {",
  "    pm.expect(pm.response.headers.get('Content-Type')).to.include('application/problem+json');",
  "    pm.expect(pm.response.json().type).to.eql(type);",
  "  });",
  "}",
  "function faultsListed() {",
  "  pm.test('faults listed as field/message', function () {",
  "    var e = pm.response.json().errors;",
  "    pm.expect(e).to.be.an('array').with.length.above(0);",
  "    pm.expect(e[0]).to.have.all.keys('field', 'message');",
  "  });",
  "}",
  "function machineShape(b) {",
  "  pm.expect(b).to.include.keys('id', 'slotCount', 'colours', 'k', 'playCostPence', 'floatPolicy',",
  "    'floatPence', 'reservePence', 'status', 'freePlays', 'totalPlays', 'totalPaidPence');",
  "}",
];

function req(name, method, url, body, tests, opts = {}) {
  const item = {
    name,
    request: {
      method,
      header: body !== undefined ? [{ key: "Content-Type", value: "application/json" }] : [],
      url,
    },
  };
  if (body !== undefined) {
    item.request.body = { mode: "raw", raw: typeof body === "string" ? body : JSON.stringify(body) };
  }
  item.event = [];
  if (opts.pre) {
    item.event.push({ listen: "prerequest", script: { type: "text/javascript", exec: opts.pre.split("\n") } });
  }
  item.event.push({
    listen: "test",
    script: { type: "text/javascript", exec: HELPERS.concat(tests.join("\n").split("\n")) },
  });
  return item;
}

const api = (p) => "{{baseUrl}}/api/v1/machines" + p;
const DEFAULT_MACHINE = { playCostPence: 100, startingFloatPence: 1000 };
const FULL_MACHINE = {
  slotCount: 4,
  colours: ["black", "white", "green", "yellow"],
  k: 2,
  playCostPence: 100,
  startingFloatPence: 1000,
  floatPolicy: "MIN_RESERVE",
};
const ONE_SLOT_JACKPOT = {
  slotCount: 1,
  colours: ["black"],
  k: 1,
  playCostPence: 100,
  startingFloatPence: 1000,
  floatPolicy: "MIN_RESERVE",
};
const UNKNOWN_ID = "00000000-0000-0000-0000-000000000000";

const folders = [
  {
    name: "1 Configure (BR-01, AC-01 to AC-05)",
    items: [
      req("1.1 Create machine with defaults (AC-02)", "POST", api(""), DEFAULT_MACHINE, [
        "pm.test('201 Created', function () { pm.response.to.have.status(201); });",
        "pm.test('Location header points to new machine', function () {",
        "  pm.expect(pm.response.headers.get('Location')).to.eql('/api/v1/machines/' + pm.response.json().id);",
        "});",
        "var b = pm.response.json();",
        "machineShape(b);",
        "pm.test('defaults applied: 4 slots, colours, k=2, MIN_RESERVE', function () {",
        "  pm.expect(b.slotCount).to.eql(4);",
        "  pm.expect(b.colours).to.eql(['black', 'white', 'green', 'yellow']);",
        "  pm.expect(b.k).to.eql(2);",
        "  pm.expect(b.floatPolicy).to.eql('MIN_RESERVE');",
        "});",
        "pm.test('ACTIVE, reserve 500p', function () {",
        "  pm.expect(b.status).to.eql('ACTIVE');",
        "  pm.expect(b.reservePence).to.eql(500);",
        "});",
        "pm.collectionVariables.set('machineId', b.id);",
      ]),
      req("1.2 Create machine with full settings (AC-01)", "POST", api(""), FULL_MACHINE, [
        "pm.test('201 Created', function () { pm.response.to.have.status(201); });",
        "var b = pm.response.json();",
        "machineShape(b);",
        "pm.test('exact settings and ACTIVE', function () {",
        "  pm.expect(b.slotCount).to.eql(4);",
        "  pm.expect(b.colours).to.eql(['black', 'white', 'green', 'yellow']);",
        "  pm.expect(b.k).to.eql(2);",
        "  pm.expect(b.playCostPence).to.eql(100);",
        "  pm.expect(b.floatPence).to.eql(1000);",
        "  pm.expect(b.status).to.eql('ACTIVE');",
        "});",
        "pm.test('new unique id', function () {",
        "  pm.expect(b.id).to.not.eql(pm.collectionVariables.get('machineId'));",
        "});",
      ]),
      req("1.3 Refuse k = 0 (AC-03)", "POST", api(""), Object.assign({}, FULL_MACHINE, { k: 0 }), [
        "problem(400, 'validation-error');",
        "faultsListed();",
      ]),
      req("1.4 Refuse k = 5 with 4 slots (AC-03)", "POST", api(""), Object.assign({}, FULL_MACHINE, { k: 5 }), [
        "problem(400, 'validation-error');",
        "faultsListed();",
      ]),
      req("1.5 Refuse duplicate colours (AC-03)", "POST", api(""),
        Object.assign({}, FULL_MACHINE, { colours: ["black", "black", "green", "yellow"] }), [
        "problem(400, 'validation-error');",
        "faultsListed();",
      ]),
      req("1.6 Refuse play cost 0 (AC-03)", "POST", api(""), Object.assign({}, FULL_MACHINE, { playCostPence: 0 }), [
        "problem(400, 'validation-error');",
        "faultsListed();",
      ]),
      req("1.7 Refuse negative starting float (AC-03)", "POST", api(""),
        Object.assign({}, FULL_MACHINE, { startingFloatPence: -1 }), [
        "problem(400, 'validation-error');",
        "faultsListed();",
      ]),
      req("1.8 Refuse starting float 499p under MIN_RESERVE (AC-04)", "POST", api(""),
        Object.assign({}, FULL_MACHINE, { startingFloatPence: 499 }), [
        "problem(400, 'validation-error');",
        "faultsListed();",
      ]),
      req("1.9 Accept starting float 500p under MIN_RESERVE (AC-04)", "POST", api(""),
        Object.assign({}, FULL_MACHINE, { startingFloatPence: 500 }), [
        "pm.test('201 Created', function () { pm.response.to.have.status(201); });",
        "var b = pm.response.json();",
        "pm.collectionVariables.set('machineAt500', b.id);",
        "pm.test('float 500, ACTIVE', function () {",
        "  pm.expect(b.floatPence).to.eql(500);",
        "  pm.expect(b.status).to.eql('ACTIVE');",
        "});",
      ]),
      req("1.10 Create large configuration (AC-05)", "POST", api(""),
        '{"slotCount":10,"colours":{{largeColours}},"k":3,"playCostPence":50,"startingFloatPence":1000,"floatPolicy":"MIN_RESERVE"}', [
        "pm.test('201 Created', function () { pm.response.to.have.status(201); });",
        "var b = pm.response.json();",
        "pm.test('10 slots, 300 colours, k=3, cost 50p', function () {",
        "  pm.expect(b.slotCount).to.eql(10);",
        "  pm.expect(b.colours.length).to.eql(300);",
        "  pm.expect(b.k).to.eql(3);",
        "  pm.expect(b.playCostPence).to.eql(50);",
        "});",
      ], {
        pre: "var c = []; for (var i = 0; i < 300; i++) { c.push('c' + i); }\npm.variables.set('largeColours', JSON.stringify(c));",
      }),
      req("1.11 Refuse slot count above NFR-02 limit (NFR-02)", "POST", api(""),
        { slotCount: 10001, colours: ["black", "white", "green", "yellow"], k: 2, playCostPence: 100, startingFloatPence: 1000 }, [
        "problem(400, 'validation-error');",
        "faultsListed();",
      ]),
    ],
  },
  {
    name: "2 Read state (BR-09, AC-28, AC-29)",
    items: [
      req("2.1 Read configured machine (AC-28)", "GET", api("/{{machineId}}"), undefined, [
        "pm.test('200 OK', function () { pm.response.to.have.status(200); });",
        "var b = pm.response.json();",
        "machineShape(b);",
        "pm.collectionVariables.set('startFloat', b.floatPence);",
        "pm.test('ACTIVE and no plays yet', function () {",
        "  pm.expect(b.status).to.eql('ACTIVE');",
        "  pm.expect(b.totalPlays).to.eql(0);",
        "});",
      ]),
      req("2.2 Read unknown machine (AC-29)", "GET", api("/" + UNKNOWN_ID), undefined, [
        "problem(404, 'machine-not-found');",
      ]),
    ],
  },
  {
    name: "3 Play (BR-02, BR-07, AC-07, AC-14 to AC-18, AC-26, AC-29, AC-36)",
    items: [
      req("3.1 Paid play on configured machine (AC-15)", "POST", api("/{{machineId}}/plays"), { useFreePlay: false }, [
        "pm.test('200 OK', function () { pm.response.to.have.status(200); });",
        "var b = pm.response.json();",
        "pm.test('response has every play field', function () {",
        "  pm.expect(b).to.include.keys('playId', 'slots', 'prize', 'nominalPayout', 'paidOut',",
        "    'freePlaysCredited', 'freePlayBalance', 'floatAfter', 'paidWithFreePlay', 'status');",
        "});",
        "pm.test('slots are colour names, one per slot', function () {",
        "  pm.expect(b.slots.length).to.eql(4);",
        "  b.slots.forEach(function (s) { pm.expect(['black','white','green','yellow']).to.include(s); });",
        "});",
        "pm.test('paid in full under MIN_RESERVE; no free plays (A5, A10)', function () {",
        "  pm.expect(b.paidOut).to.eql(b.nominalPayout);",
        "  pm.expect(b.freePlaysCredited).to.eql(0);",
        "  pm.expect(b.paidWithFreePlay).to.eql(false);",
        "});",
        "pm.test('float arithmetic: before + cost - paid = after (BR-02)', function () {",
        "  pm.expect(b.floatAfter).to.eql(Number(pm.collectionVariables.get('startFloat')) + 100 - b.paidOut);",
        "});",
        "pm.collectionVariables.set('floatAfterPlay1', b.floatAfter);",
      ]),
      req("3.2 Read state after paid play (AC-30 totals)", "GET", api("/{{machineId}}"), undefined, [
        "pm.test('200 OK', function () { pm.response.to.have.status(200); });",
        "var b = pm.response.json();",
        "pm.test('one play counted; float matches last play', function () {",
        "  pm.expect(b.totalPlays).to.eql(1);",
        "  pm.expect(b.floatPence).to.eql(pm.collectionVariables.get('floatAfterPlay1'));",
        "});",
      ]),
      req("3.3 Play at exactly the reserve (AC-17)", "POST", api("/{{machineAt500}}/plays"), { useFreePlay: false }, [
        "pm.test('accepted: 200 OK', function () { pm.response.to.have.status(200); });",
        "var b = pm.response.json();",
        "pm.test('float 500 + 100 - paid; no free plays credited', function () {",
        "  pm.expect(b.floatAfter).to.eql(500 + 100 - b.paidOut);",
        "  pm.expect(b.freePlaysCredited).to.eql(0);",
        "  pm.expect(b.paidOut).to.eql(b.nominalPayout);",
        "});",
      ]),
      req("3.4 Create one-slot machine for jackpot (AC-14)", "POST", api(""), ONE_SLOT_JACKPOT, [
        "pm.test('201 Created', function () { pm.response.to.have.status(201); });",
        "pm.collectionVariables.set('jackpotMachineId', pm.response.json().id);",
      ]),
      req("3.5 Jackpot play pays whole float and drains machine (AC-07, AC-18)", "POST", api("/{{jackpotMachineId}}/plays"),
        { useFreePlay: false }, [
        "pm.test('200 OK', function () { pm.response.to.have.status(200); });",
        "var b = pm.response.json();",
        "pm.test('JACKPOT: whole float incl. stake paid, float 0, NEEDS_REFILL', function () {",
        "  pm.expect(b.prize).to.eql('JACKPOT');",
        "  pm.expect(b.paidOut).to.eql(1100);",
        "  pm.expect(b.floatAfter).to.eql(0);",
        "  pm.expect(b.status).to.eql('NEEDS_REFILL');",
        "  pm.expect(b.freePlaysCredited).to.eql(0);",
        "});",
      ]),
      req("3.6 Refuse play below reserve (AC-16)", "POST", api("/{{jackpotMachineId}}/plays"), { useFreePlay: false }, [
        "problem(409, 'machine-needs-refill');",
        "var b = pm.response.json();",
        "pm.test('states float, reserve and status', function () {",
        "  pm.expect(b.floatPence).to.eql(0);",
        "  pm.expect(b.reservePence).to.eql(500);",
        "  pm.expect(b.machineStatus).to.eql('NEEDS_REFILL');",
        "});",
      ]),
      req("3.7 Read drained machine: refusal changed nothing (AC-16)", "GET", api("/{{jackpotMachineId}}"), undefined, [
        "pm.test('200 OK', function () { pm.response.to.have.status(200); });",
        "var b = pm.response.json();",
        "pm.test('float 0, one play counted, no stake taken by refused play', function () {",
        "  pm.expect(b.floatPence).to.eql(0);",
        "  pm.expect(b.totalPlays).to.eql(1);",
        "  pm.expect(b.totalPaidPence).to.eql(1100);",
        "  pm.expect(b.status).to.eql('NEEDS_REFILL');",
        "});",
      ]),
      req("3.8 Refill drained machine (AC-18)", "POST", api("/{{jackpotMachineId}}/refills"), { amountPence: 1000 }, [
        "pm.test('200 OK', function () { pm.response.to.have.status(200); });",
        "var b = pm.response.json();",
        "pm.test('float 1000 and ACTIVE', function () {",
        "  pm.expect(b.floatPence).to.eql(1000);",
        "  pm.expect(b.status).to.eql('ACTIVE');",
        "});",
      ]),
      req("3.9 Play again after refill (AC-18)", "POST", api("/{{jackpotMachineId}}/plays"), { useFreePlay: false }, [
        "pm.test('accepted after refill: 200 OK', function () { pm.response.to.have.status(200); });",
      ]),
      req("3.10 Refuse free play with no credit (AC-26)", "POST", api("/{{machineId}}/plays"), { useFreePlay: true }, [
        "problem(409, 'no-free-plays');",
      ]),
      req("3.11 Refuse malformed useFreePlay value (AC-36)", "POST", api("/{{machineId}}/plays"), '{"useFreePlay":"yes"}', [
        "problem(400, 'validation-error');",
        "faultsListed();",
      ]),
      req("3.12 Refuse unreadable JSON body (AC-36)", "POST", api("/{{machineId}}/plays"), "{", [
        "problem(400, 'validation-error');",
        "faultsListed();",
      ]),
      req("3.13 Refuse missing useFreePlay field (AC-36)", "POST", api("/{{machineId}}/plays"), "{}", [
        "problem(400, 'validation-error');",
        "faultsListed();",
      ]),
      req("3.14 Play unknown machine (AC-29)", "POST", api("/" + UNKNOWN_ID + "/plays"), { useFreePlay: false }, [
        "problem(404, 'machine-not-found');",
      ]),
    ],
  },
  {
    name: "4 Refill (BR-08, AC-19 to AC-21)",
    items: [
      req("4.1 Create policy NONE machine with float 0 (AC-19 setup)", "POST", api(""),
        Object.assign({}, FULL_MACHINE, { startingFloatPence: 0, floatPolicy: "NONE" }), [
        "pm.test('201 Created', function () { pm.response.to.have.status(201); });",
        "var b = pm.response.json();",
        "pm.collectionVariables.set('noneMachineId', b.id);",
        "pm.test('float 0, NONE is always ACTIVE (BR-09)', function () {",
        "  pm.expect(b.floatPence).to.eql(0);",
        "  pm.expect(b.status).to.eql('ACTIVE');",
        "});",
      ]),
      req("4.2 Refill empty machine (AC-19)", "POST", api("/{{noneMachineId}}/refills"), { amountPence: 5000 }, [
        "pm.test('200 OK', function () { pm.response.to.have.status(200); });",
        "var b = pm.response.json();",
        "pm.test('float 5000, ACTIVE', function () {",
        "  pm.expect(b.floatPence).to.eql(5000);",
        "  pm.expect(b.status).to.eql('ACTIVE');",
        "});",
      ]),
      req("4.3 Refuse zero refill (AC-20)", "POST", api("/{{noneMachineId}}/refills"), { amountPence: 0 }, [
        "problem(400, 'validation-error');",
        "faultsListed();",
      ]),
      req("4.4 Refuse negative refill (AC-20)", "POST", api("/{{noneMachineId}}/refills"), { amountPence: -100 }, [
        "problem(400, 'validation-error');",
        "faultsListed();",
      ]),
      req("4.5 Read after refused refills: unchanged (AC-20)", "GET", api("/{{noneMachineId}}"), undefined, [
        "pm.test('float still 5000', function () {",
        "  pm.expect(pm.response.json().floatPence).to.eql(5000);",
        "});",
      ]),
      req("4.6 Refill unknown machine (AC-21)", "POST", api("/" + UNKNOWN_ID + "/refills"), { amountPence: 100 }, [
        "problem(404, 'machine-not-found');",
      ]),
    ],
  },
];

let idCounter = 0;
function assignIds(items) {
  items.forEach((it) => {
    if (it.item) assignIds(it.item);
    idCounter += 1;
    it._postman_id = "fm-" + String(idCounter).padStart(3, "0");
  });
}

const collection = {
  info: {
    _postman_id: "fruitmachine-api-us04",
    name: "Fruit Machine API (Issue 4)",
    description:
      "Acceptance checks for Issue 4 REST API, per docs/plans/US-04-rest-api-plan.md section 7. " +
      "Run in order (folders depend on earlier collection variables). " +
      "Not covered over HTTP: AC-25 and AC-27 (free-play credit needs a shortfall, which is not reproducible without forced spins; covered by Issue 7 system tests). " +
      "503 machine-busy and AC-32 (lock timeout) need concurrent load; covered by Issue 5. " +
      "Jackpot cases use a one-slot, one-colour machine (certain outcome). " +
      "Run: newman run postman/fruitmachine-api.postman_collection.json --env-var baseUrl=http://localhost:8080",
    schema: "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
  },
  variable: [
    { key: "baseUrl", value: "http://localhost:8080" },
    { key: "machineId", value: "" },
    { key: "machineAt500", value: "" },
    { key: "jackpotMachineId", value: "" },
    { key: "noneMachineId", value: "" },
    { key: "startFloat", value: "" },
    { key: "floatAfterPlay1", value: "" },
  ],
  item: folders.map((f) => ({ name: f.name, item: f.items })),
};

assignIds(collection.item);
fs.mkdirSync(path.dirname(OUT), { recursive: true });
fs.writeFileSync(OUT, JSON.stringify(collection, null, 2) + "\n");
console.log("wrote " + OUT + " with " + idCounter + " items");
