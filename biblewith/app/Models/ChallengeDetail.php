<?php

namespace App\Models;

class ChallengeDetail extends \CodeIgniter\Model
{
    protected $table = 'ChallengeDetail';
    protected $allowedFields = [
        'chal_no',
        'bible_no',
        'progress_day',
        'is_checked',
        'start_date',
        'progress_date',
        'chal_detail_no'
    ];
    protected $primaryKey = 'chal_detail_no';
}

