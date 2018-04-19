const { Chromeless } = require('chromeless')

module.exports = (content, callback) => {

  async function run(accountId, amount) {

    const chromeless = new Chromeless({
      launchChrome: false,
      cdp: { host: 'chrome', port: 9222, secure: false, closeTab: true }
    })

    const screenshot = await chromeless
      .goto('http://hivemind/Login/logout')
      .click('#TestLoginLink_button')
      .wait('.btn-danger')
      .goto('http://hivemind/vapps/hmadmin/Accounting/FinancialAccount/FinancialAccountTrans?finAccountId=' + accountId)
      .wait('#AdjustDialog-button')
      .click('#AdjustDialog-button')
      .type(amount, '#AdjustFinancialAccount_amount')
      .mousedown('#select2-AdjustFinancialAccount_reasonEnumId-container')
      .mouseup('#select2-AdjustFinancialAccount_reasonEnumId-container')
      .press(40, 5)
      .press(13)
      .click('#AdjustFinancialAccount_submitButton')
      .screenshot()
      .catch(e => {
        console.log('{"error":"' + e.message + '"}')
        process.exit(1);
      })

    console.log('{"success": "ok", "screenshot":"' + screenshot + '"}')

    await chromeless.end()
  }

  const opt = JSON.parse(content)
  run(opt.accountId, opt.amount).catch(console.error.bind(console))

};
