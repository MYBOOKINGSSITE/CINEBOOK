CineBook — BookMyShow Affiliate Site (Static)

How to use
----------
1) Open index.html in any editor.
2) In the CONFIG section at the bottom of the file:
   - DEFAULT_CITY: change default city.
   - AFFILIATE_WRAPPER: paste your affiliate deep-link wrapper if your network gives one.
     Example (Impact):
     https://YOURPARTNERLINK.example.net/c/XXXXXX/XXXXXX/1234?subId={YOURSUB}&u=
     Keep it "" if you will paste full deep links per item.
   - ITEMS: replace sample items with your movies/events.
     For each item, set 'link' to your BookMyShow deep-link (with tracking), OR set a normal BMS URL and rely on AFFILIATE_WRAPPER.

3) Deploy
   - Netlify: drag-and-drop the folder or run `netlify deploy`.
   - Vercel: `vercel` and select this folder.
   - cPanel/Shared Hosting: upload files to `public_html`.

Notes
-----
- This is a simple, fast static site. No server required.
- We don’t own BookMyShow; names/logos used for reference only.
- Feel free to rename the brand in <title> and header.
