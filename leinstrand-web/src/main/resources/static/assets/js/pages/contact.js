var Contact = function() {

    return {

        // Map
        initMap : function() {
            var map;
            $(document).ready(function() {
                map = new GMaps({
                    div : '#map',
                    lat : 63.3439,
                    lng : 10.3110,
                    zoom: 12
                });
                var kunstgressbanen = map.addMarker({
                    lat : 63.3237,
                    lng : 10.3143,
                    title : 'Kunsgressbanen',
                    infoWindow: {
                        content: '<strong>Kunstgressbanen</strong> er en 9-er bane med flombelysning.'
                    }
                });
                var skoytebanen = map.addMarker({
                    lat : 63.3244,
                    lng : 10.3128,
                    title : 'Skøytebanen',
                    infoWindow: {
                        content: '<strong>Skøytebanen</strong> anlegges på grusplassen ved Samfunnshuset om vinteren.'
                    }
                });
                var bygdarommet = map.addMarker({
                    lat : 63.3314,
                    lng : 10.2994,
                    title : 'Gymsalen / Bygdarommet',
                    infoWindow: {
                        content: '<strong>Gymsalen / Bygdarommet</strong> er en del av Nypvang skole.'
                    }
                });
                var samfunnshuset = map.addMarker({
                    lat : 63.3245,
                    lng : 10.3115,
                    title : 'Samfunnshuset',
                    infoWindow: {
                        content: '<strong>Samfunnshuset</strong> huser mange av bygdas større arrangementer.'
                    }
                });
                var skoytebua = map.addMarker({
                    lat : 63.3240,
                    lng : 10.3125,
                    title : 'Skøytebua',
                    infoWindow: {
                        content: '<strong>Skøytebua</strong> er ei varmestue og kjøkken som bl.a benyttes ved '
                               + 'arrangementer på skøytebanen.'
                    }
                });
                var kletthallen = map.addMarker({
                    lat : 63.3233,
                    lng : 10.3197,
                    title : 'Kletthallen',
                    infoWindow: {
                        content: '<strong>Kletthallen</strong> er en gammel ridehall hvor det er lagt kunstgress. '
                               + 'Adkomst til fots lags gang og sykkelveien forbi samfunnshuset. Det er ikke '
                               + 'tillatt å kjøre helt frem til hallen.'
                    }
                });
                var skistadion = map.addMarker({
                    lat : 63.3557,
                    lng : 10.2517,
                    title : 'Skistadion',
                    infoWindow: {
                        content: '<strong>Skistadion</strong> går også under navnet Ringvål. '
                               + 'Parkeringsavgift i helgene i skisesongen.'
                    }
                });
            });
        }

    };
}();