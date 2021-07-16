(function cardContent(){
    $(".card h3").attr("onclick", "cardDisplayChange(this)")
})();

function cardDisplayChange(card) {
    let cc = $(card).next(".card-content")
    let s = cc.attr("style")
    if (s === undefined || s.indexOf("block") !== -1) {
        cc.attr("style", "display: none;")
    } else if (s.indexOf("none") !== -1) {
        cc.attr("style", "display: block;")
    }
}