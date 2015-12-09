/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

/**
 * Javascript for the preview functionality on the reporter pages.
 * Will change the extension of the action URL and process the JSON
 * response to display on the page the paths to the resources or error
 * messages.
 */
$(function() {
    $('body').on('click', '#preview-request-submit ', function(e) {

        var $this = $(this),
            $form = $this.closest('form'),
            json,
            i,
            actionUrl = $form.attr('action').replace(".csv",".json");

        $('.notification').fadeOut();

        $.post(actionUrl, {'preview': $this.attr('name') === 'preview' }, function(data) {
            $('.notification').hide();

            if(data.status === 'preview') {
                /* Preview */

                $('.notification.preview .filters').html('');
                if(!data.resourceList) {
                    $('.notification.preview .filters').append('<li>No matching resources found.</li>');
                } else {
                    for(i = 0; i < data.resourceList.length; i++) {
                        $('.notification.preview .filters').append('<li>' + data.resourceList[i].resourcePath + '</li>');
                    }
                }

                $('.notification.preview').fadeIn(); 
            } else {
                /* Error */
                $('.notification.error .msg').text(data.msg || '');
                $('.notification.error').fadeIn();
            }

            $('html, body').animate({
                scrollTop: $('.notifications').offset().top - 20
            }, 500);
        }, 'json');

        e.preventDefault();
        //return false;
    });
});
